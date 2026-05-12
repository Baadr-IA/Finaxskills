package com.finaxys.skillsrh.service.assessment;

import com.finaxys.skillsrh.api.ApiException;
import com.finaxys.skillsrh.config.QuizGenerationProperties;
import com.finaxys.skillsrh.domain.AssessmentSession;
import com.finaxys.skillsrh.domain.AssessmentSessionStatus;
import com.finaxys.skillsrh.domain.Collaborator;
import com.finaxys.skillsrh.domain.CollaboratorSkill;
import com.finaxys.skillsrh.domain.CollaboratorSkillId;
import com.finaxys.skillsrh.domain.Skill;
import com.finaxys.skillsrh.domain.StartLevelSource;
import com.finaxys.skillsrh.repository.AssessmentSessionRepository;
import com.finaxys.skillsrh.repository.CollaboratorRepository;
import com.finaxys.skillsrh.repository.CollaboratorSkillRepository;
import com.finaxys.skillsrh.repository.SkillRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Transactional
public class AdaptiveAssessmentSessionService {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final SkillRepository skillRepository;
    private final CollaboratorSkillRepository collaboratorSkillRepository;
    private final QuizGenerationClient quizGenerationClient;
    private final QuizGenerationProperties properties;
    private final ObjectMapper objectMapper;

    public AdaptiveAssessmentSessionService(
        AssessmentSessionRepository assessmentSessionRepository,
        CollaboratorRepository collaboratorRepository,
        SkillRepository skillRepository,
        CollaboratorSkillRepository collaboratorSkillRepository,
        QuizGenerationClient quizGenerationClient,
        QuizGenerationProperties properties,
        ObjectMapper objectMapper
    ) {
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.skillRepository = skillRepository;
        this.collaboratorSkillRepository = collaboratorSkillRepository;
        this.quizGenerationClient = quizGenerationClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public QuizModels.AssessmentSessionState startSession(StartSessionCommand command) {
        Collaborator collaborator = requireCollaborator(command.collaboratorId());
        Skill skill = requireSkill(command.skillId());
        int startingLevel = resolveStartingLevel(command.collaboratorId(), command.skillId(), command.startLevelSource());
        String skillKey = normalizeSkillKey(skill.getName());

        AssessmentSession session = new AssessmentSession(
            collaborator,
            skill,
            skill.getName(),
            skillKey,
            command.startLevelSource(),
            startingLevel,
            startingLevel,
            properties.getTotalBlocks(),
            properties.getBlockQuestionCount(),
            normalizeInstructions(command.instructions()),
            AssessmentSessionStatus.IN_PROGRESS,
            "[]"
        );

        QuizModels.SessionBlock firstBlock = createBlock(skillKey, startingLevel, 1, session.getGenerationInstructions());
        session.setBlocksPayload(serializeBlocks(List.of(firstBlock)));
        session.touch();

        AssessmentSession saved = assessmentSessionRepository.save(session);
        return toState(saved, List.of(firstBlock));
    }

    public QuizModels.AssessmentSessionState submitAnswers(Long sessionId, SubmitAnswersCommand command) {
        AssessmentSession session = requireSession(sessionId);
        if (session.getStatus() == AssessmentSessionStatus.COMPLETED) {
            throw new ApiException(HttpStatus.CONFLICT, "assessment-session-completed",
                "This assessment session is already completed");
        }

        List<QuizModels.SessionBlock> blocks = deserializeBlocks(session.getBlocksPayload());
        QuizModels.SessionBlock currentBlock = blocks.stream()
            .filter(block -> block.blockNumber() == command.blockNumber())
            .findFirst()
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "assessment-block-not-found",
                "Assessment block not found"));

        if (currentBlock.status() == QuizModels.SessionBlockStatus.ANSWERED) {
            throw new ApiException(HttpStatus.CONFLICT, "assessment-block-already-answered",
                "This assessment block has already been answered");
        }

        validateSubmittedAnswers(currentBlock, command.answers());
        Map<String, String> expectedAnswers = new LinkedHashMap<>();
        currentBlock.expectedAnswers().forEach(answer -> expectedAnswers.put(answer.questionId(), answer.option()));

        int correctAnswers = (int) command.answers().stream()
            .filter(answer -> expectedAnswers.get(answer.questionId()).equalsIgnoreCase(answer.option()))
            .count();
        int scorePercentage = correctAnswers * 100 / currentBlock.questions().size();
        int nextLevel = determineNextLevel(currentBlock.level(), scorePercentage);
        QuizModels.LevelTransition transition = transitionFrom(currentBlock.level(), nextLevel);

        QuizModels.SessionBlock answeredBlock = new QuizModels.SessionBlock(
            currentBlock.blockNumber(),
            currentBlock.level(),
            currentBlock.title(),
            currentBlock.questions(),
            currentBlock.expectedAnswers(),
            currentBlock.difficulty(),
            currentBlock.durationMinutes(),
            currentBlock.evaluationCriteria(),
            currentBlock.generationSource(),
            currentBlock.fallbackReason(),
            QuizModels.SessionBlockStatus.ANSWERED,
            command.answers(),
            correctAnswers,
            scorePercentage,
            transition
        );

        List<QuizModels.SessionBlock> updatedBlocks = new ArrayList<>(blocks);
        updatedBlocks.set(currentBlock.blockNumber() - 1, answeredBlock);

        if (currentBlock.blockNumber() >= session.getTotalBlocks()) {
            session.setCurrentLevel(nextLevel);
            session.setStatus(AssessmentSessionStatus.COMPLETED);
            session.setBlocksPayload(serializeBlocks(updatedBlocks));
            session.touch();
            return toState(assessmentSessionRepository.save(session), updatedBlocks);
        }

        QuizModels.SessionBlock nextBlock = createBlock(
            session.getSkillKey(),
            nextLevel,
            currentBlock.blockNumber() + 1,
            session.getGenerationInstructions()
        );
        updatedBlocks.add(nextBlock);
        session.setCurrentLevel(nextLevel);
        session.setBlocksPayload(serializeBlocks(updatedBlocks));
        session.touch();
        return toState(assessmentSessionRepository.save(session), updatedBlocks);
    }

    @Transactional(readOnly = true)
    public QuizModels.AssessmentSessionState getSession(Long sessionId) {
        AssessmentSession session = requireSession(sessionId);
        return toState(session, deserializeBlocks(session.getBlocksPayload()));
    }

    private Collaborator requireCollaborator(Long collaboratorId) {
        return collaboratorRepository.findById(collaboratorId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "collaborator-not-found", "Collaborator not found"));
    }

    private Skill requireSkill(Long skillId) {
        return skillRepository.findById(skillId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "skill-not-found", "Skill not found"));
    }

    private AssessmentSession requireSession(Long sessionId) {
        return assessmentSessionRepository.findById(sessionId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "assessment-session-not-found",
                "Assessment session not found"));
    }

    private int resolveStartingLevel(Long collaboratorId, Long skillId, StartLevelSource source) {
        CollaboratorSkillId id = new CollaboratorSkillId(collaboratorId, skillId);
        CollaboratorSkill collaboratorSkill = collaboratorSkillRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "missing-skill-assessment",
                "No skill assessment exists for the selected collaborator and skill"));

        Integer level = source == StartLevelSource.HR ? collaboratorSkill.getHrLevel() : collaboratorSkill.getSelfLevel();
        if (level == null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "missing-start-level",
                "The selected start level source is not available for this collaborator");
        }
        return level;
    }

    private QuizModels.SessionBlock createBlock(String skillKey, int level, int blockNumber, String instructions) {
        QuizModels.GeneratedQuizBlock generated = quizGenerationClient.generateBlock(
            new QuizModels.QuizGenerationRequest(skillKey, level, properties.getBlockQuestionCount(), instructions)
        );

        return new QuizModels.SessionBlock(
            blockNumber,
            level,
            generated.title(),
            generated.questions(),
            generated.expectedAnswers(),
            generated.difficulty(),
            generated.durationMinutes(),
            generated.evaluationCriteria(),
            generated.generationSource(),
            generated.fallbackReason(),
            QuizModels.SessionBlockStatus.ACTIVE,
            List.of(),
            null,
            null,
            null
        );
    }

    private void validateSubmittedAnswers(QuizModels.SessionBlock block, List<QuizModels.SubmittedAnswer> answers) {
        if (answers == null || answers.size() != block.questions().size()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "invalid-block-answers",
                "Each question in the block must have exactly one answer");
        }

        Map<String, String> submitted = new LinkedHashMap<>();
        for (QuizModels.SubmittedAnswer answer : answers) {
            if (answer.questionId() == null || answer.option() == null) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "invalid-block-answers",
                    "Each answer must contain a questionId and an option");
            }
            if (block.questions().stream().noneMatch(question -> question.id().equals(answer.questionId()))) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "invalid-block-answers",
                    "An answer targets an unknown question");
            }
            String normalizedOption = answer.option().trim().toUpperCase();
            if (!List.of("A", "B", "C", "D").contains(normalizedOption)) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "invalid-block-answers",
                    "An answer contains an invalid option");
            }
            if (submitted.put(answer.questionId(), normalizedOption) != null) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "invalid-block-answers",
                    "Each question can be answered only once");
            }
        }
    }

    private int determineNextLevel(int currentLevel, int scorePercentage) {
        if (scorePercentage >= 80) {
            return Math.min(5, currentLevel + 1);
        }
        if (scorePercentage < 50) {
            return Math.max(1, currentLevel - 1);
        }
        return currentLevel;
    }

    private QuizModels.LevelTransition transitionFrom(int currentLevel, int nextLevel) {
        if (nextLevel > currentLevel) {
            return QuizModels.LevelTransition.UP;
        }
        if (nextLevel < currentLevel) {
            return QuizModels.LevelTransition.DOWN;
        }
        return QuizModels.LevelTransition.STAY;
    }

    private String normalizeSkillKey(String skillName) {
        String lowerCase = skillName.toLowerCase();
        return NON_ALPHANUMERIC.matcher(lowerCase).replaceAll("_").replaceAll("^_+|_+$", "");
    }

    private String normalizeInstructions(String instructions) {
        if (instructions == null || instructions.isBlank()) {
            return null;
        }
        return instructions.trim();
    }

    private String serializeBlocks(List<QuizModels.SessionBlock> blocks) {
        try {
            return objectMapper.writeValueAsString(blocks);
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "assessment-session-serialization-error",
                "Unable to serialize assessment blocks");
        }
    }

    private List<QuizModels.SessionBlock> deserializeBlocks(String blocksPayload) {
        try {
            return objectMapper.readValue(blocksPayload, new TypeReference<>() {});
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "assessment-session-deserialization-error",
                "Unable to deserialize assessment blocks");
        }
    }

    private QuizModels.AssessmentSessionState toState(AssessmentSession session, List<QuizModels.SessionBlock> blocks) {
        return new QuizModels.AssessmentSessionState(
            session.getId(),
            session.getCollaborator().getId(),
            session.getCollaborator().getFirstName() + " " + session.getCollaborator().getLastName(),
            session.getSkill().getId(),
            session.getSkillNameSnapshot(),
            session.getSkillKey(),
            session.getStartLevelSource(),
            session.getStartingLevel(),
            session.getCurrentLevel(),
            session.getTotalBlocks(),
            session.getQuestionCountPerBlock(),
            session.getGenerationInstructions(),
            session.getStatus(),
            session.getCreatedAt(),
            session.getUpdatedAt(),
            blocks
        );
    }

    public record StartSessionCommand(
        Long collaboratorId,
        Long skillId,
        StartLevelSource startLevelSource,
        String instructions
    ) {}

    public record SubmitAnswersCommand(
        int blockNumber,
        List<QuizModels.SubmittedAnswer> answers
    ) {}
}
