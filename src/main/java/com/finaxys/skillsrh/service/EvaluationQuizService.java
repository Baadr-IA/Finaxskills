package com.finaxys.skillsrh.service;

import com.finaxys.skillsrh.api.ApiException;
import com.finaxys.skillsrh.domain.AnswerOption;
import com.finaxys.skillsrh.domain.CollaboratorAnswer;
import com.finaxys.skillsrh.domain.Skill;
import com.finaxys.skillsrh.domain.Status;
import com.finaxys.skillsrh.domain.TestCollab;
import com.finaxys.skillsrh.domain.Question;
import com.finaxys.skillsrh.repository.AnswerOptionRepository;
import com.finaxys.skillsrh.repository.CollaboratorAnswerRepository;
import com.finaxys.skillsrh.repository.QuestionRepository;
import com.finaxys.skillsrh.repository.TestCollabRepository;
import com.finaxys.skillsrh.service.assessment.QuizGenerationClient;
import com.finaxys.skillsrh.service.assessment.QuizModels;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class EvaluationQuizService {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");

    private final TestCollabRepository testCollabRepository;
    private final QuestionRepository questionRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final CollaboratorAnswerRepository collaboratorAnswerRepository;
    private final QuizGenerationClient quizGenerationClient;

    public EvaluationQuizService(
        TestCollabRepository testCollabRepository,
        QuestionRepository questionRepository,
        AnswerOptionRepository answerOptionRepository,
        CollaboratorAnswerRepository collaboratorAnswerRepository,
        QuizGenerationClient quizGenerationClient
    ) {
        this.testCollabRepository = testCollabRepository;
        this.questionRepository = questionRepository;
        this.answerOptionRepository = answerOptionRepository;
        this.collaboratorAnswerRepository = collaboratorAnswerRepository;
        this.quizGenerationClient = quizGenerationClient;
    }

    @Transactional
    public StartEvaluationResponse start(Long assignmentId, Long collaboratorId, Skill skill, int level) {
        TestCollab assignment = testCollabRepository.findByIdAndCollaborator_Id(assignmentId, collaboratorId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "evaluation-assignment-not-found",
                "Evaluation assignment not found for current collaborator"));

        QuizModels.GeneratedQuizBlock generated = quizGenerationClient.generateBlock(
            new QuizModels.QuizGenerationRequest(normalizeSkillKey(skill.getName()), level, 5, null)
        );

        List<Long> existingQuestionIds = questionRepository.findByTestCollab_IdOrderByPositionOrderAsc(assignment.getId()).stream()
            .map(Question::getId)
            .toList();
        if (!existingQuestionIds.isEmpty()) {
            collaboratorAnswerRepository.deleteByCollaborator_IdAndQuestion_TestCollab_Id(collaboratorId, assignment.getId());
            answerOptionRepository.deleteByQuestion_IdIn(existingQuestionIds);
        }
        questionRepository.deleteByTestCollab_Id(assignment.getId());

        Map<String, String> expected = new LinkedHashMap<>();
        generated.expectedAnswers().forEach(answer -> expected.put(answer.questionId(), answer.option()));

        List<StartQuestionResponse> questions = new ArrayList<>();
        for (int i = 0; i < generated.questions().size(); i++) {
            QuizModels.GeneratedQuestion generatedQuestion = generated.questions().get(i);
            Question question = questionRepository.save(new Question(
                assignment,
                generatedQuestion.text(),
                i + 1
            ));

            String expectedCode = expected.getOrDefault(generatedQuestion.id(), "");
            List<StartOptionResponse> options = new ArrayList<>();
            options.add(saveOption(question, "A", generatedQuestion.optionA(), expectedCode));
            options.add(saveOption(question, "B", generatedQuestion.optionB(), expectedCode));
            options.add(saveOption(question, "C", generatedQuestion.optionC(), expectedCode));
            options.add(saveOption(question, "D", generatedQuestion.optionD(), expectedCode));

            questions.add(new StartQuestionResponse(question.getId(), generatedQuestion.text(), options));
        }

        assignment.setGeneratedAt(Instant.now());
        assignment.setStatus(Status.EN_COURS);
        assignment.setProgress(0);
        assignment.setScore(null);
        assignment.touch();
        testCollabRepository.save(assignment);

        return new StartEvaluationResponse(assignment.getEvaluation().getEvaluationName(), questions);
    }

    @Transactional
    public SubmitEvaluationResponse submitAnswers(Long assignmentId, Long collaboratorId, List<SubmitAnswerRequest> answers) {
        TestCollab assignment = testCollabRepository.findByIdAndCollaborator_Id(assignmentId, collaboratorId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "evaluation-assignment-not-found",
                "Evaluation assignment not found for current collaborator"));

        List<Question> questions = questionRepository.findByTestCollab_IdOrderByPositionOrderAsc(assignment.getId());
        if (questions.isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "quiz-not-generated",
                "No generated quiz exists for this assignment");
        }

        Map<Integer, String> answersByIndex = new LinkedHashMap<>();
        List<SubmitAnswerRequest> safeAnswers = answers != null ? answers : List.of();
        for (SubmitAnswerRequest answer : safeAnswers) {
            if (answer == null || answer.questionIndex() == null || answer.optionCode() == null) {
                continue;
            }
            answersByIndex.put(answer.questionIndex(), answer.optionCode().trim().toUpperCase(Locale.ROOT));
        }

        collaboratorAnswerRepository.deleteByCollaborator_IdAndQuestion_TestCollab_Id(collaboratorId, assignment.getId());

        int correctAnswers = 0;
        int answeredQuestions = 0;
        for (int index = 0; index < questions.size(); index++) {
            String optionCode = answersByIndex.get(index + 1);
            if (optionCode == null || optionCode.isBlank()) {
                continue;
            }
            Question question = questions.get(index);
            int questionNumber = index + 1;
            AnswerOption option = answerOptionRepository.findByQuestion_IdAndOptionCodeIgnoreCase(question.getId(), optionCode)
                .orElseThrow(() -> new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "answer-option-invalid",
                    "Selected option does not exist for question " + questionNumber));

            collaboratorAnswerRepository.save(new CollaboratorAnswer(
                assignment.getCollaborator(),
                question,
                option,
                Instant.now()
            ));
            answeredQuestions++;
            if (Boolean.TRUE.equals(option.getIsCorrect())) {
                correctAnswers++;
            }
        }

        int totalQuestions = questions.size();
        int progress = totalQuestions == 0 ? 0 : Math.round((answeredQuestions * 100f) / totalQuestions);
        int score = totalQuestions == 0 ? 0 : Math.round((correctAnswers * 100f) / totalQuestions);

        assignment.setProgress(progress);
        assignment.setScore(score);
        assignment.setStatus(answeredQuestions >= totalQuestions ? Status.COMPLETE : Status.EN_COURS);
        assignment.touch();
        testCollabRepository.save(assignment);

        return new SubmitEvaluationResponse(score, answeredQuestions, correctAnswers, totalQuestions, assignment.getStatus().getLabel());
    }

    private StartOptionResponse saveOption(Question question, String code, String text, String expectedCode) {
        boolean correct = code.equalsIgnoreCase(expectedCode);
        AnswerOption saved = answerOptionRepository.save(new AnswerOption(question, text, correct, code));
        return new StartOptionResponse(saved.getOptionCode(), saved.getAnswerText(), saved.getIsCorrect());
    }

    private String normalizeSkillKey(String skillName) {
        String lowerCase = skillName.toLowerCase(Locale.ROOT);
        return NON_ALPHANUMERIC.matcher(lowerCase).replaceAll("_").replaceAll("^_+|_+$", "");
    }

    public record StartEvaluationResponse(
        String quizTitle,
        List<StartQuestionResponse> questions
    ) {}

    public record StartQuestionResponse(
        Long questionId,
        String question,
        List<StartOptionResponse> options
    ) {}

    public record StartOptionResponse(
        String code,
        String text,
        Boolean correct
    ) {}

    public record SubmitAnswerRequest(
        Integer questionIndex,
        String optionCode
    ) {}

    public record SubmitEvaluationResponse(
        Integer score,
        Integer answeredQuestions,
        Integer correctAnswers,
        Integer totalQuestions,
        String status
    ) {}
}
