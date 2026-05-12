package com.finaxys.skillsrh.service.assessment;

import com.finaxys.skillsrh.api.ApiException;
import com.finaxys.skillsrh.config.QuizGenerationProperties;
import com.finaxys.skillsrh.domain.AssessmentSession;
import com.finaxys.skillsrh.domain.AssessmentSessionStatus;
import com.finaxys.skillsrh.domain.Collaborator;
import com.finaxys.skillsrh.domain.CollaboratorSkill;
import com.finaxys.skillsrh.domain.CollaboratorSkillId;
import com.finaxys.skillsrh.domain.Skill;
import com.finaxys.skillsrh.domain.SkillCategory;
import com.finaxys.skillsrh.domain.StartLevelSource;
import com.finaxys.skillsrh.repository.AssessmentSessionRepository;
import com.finaxys.skillsrh.repository.CollaboratorRepository;
import com.finaxys.skillsrh.repository.CollaboratorSkillRepository;
import com.finaxys.skillsrh.repository.SkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdaptiveAssessmentSessionServiceTest {

    @Mock
    private AssessmentSessionRepository assessmentSessionRepository;

    @Mock
    private CollaboratorRepository collaboratorRepository;

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private CollaboratorSkillRepository collaboratorSkillRepository;

    @Mock
    private QuizGenerationClient quizGenerationClient;

    private AdaptiveAssessmentSessionService service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        QuizGenerationProperties properties = new QuizGenerationProperties();
        properties.setBlockQuestionCount(5);
        properties.setTotalBlocks(5);

        service = new AdaptiveAssessmentSessionService(
            assessmentSessionRepository,
            collaboratorRepository,
            skillRepository,
            collaboratorSkillRepository,
            quizGenerationClient,
            properties,
            objectMapper
        );
    }

    @Test
    void startSessionUsesSelectedStartingLevelAndCreatesFirstBlock() {
        Collaborator collaborator = new Collaborator("Alice", "Martin", "alice@test.com", "Developer", "kc-alice");
        Skill skill = new Skill("Python", "Python skill", new SkillCategory("Backend", "Backend"));
        CollaboratorSkill collaboratorSkill = new CollaboratorSkill(collaborator, skill);
        collaboratorSkill.setHrLevel(3);
        collaboratorSkill.setSelfLevel(2);

        when(collaboratorRepository.findById(1L)).thenReturn(Optional.of(collaborator));
        when(skillRepository.findById(10L)).thenReturn(Optional.of(skill));
        when(collaboratorSkillRepository.findById(any(CollaboratorSkillId.class))).thenReturn(Optional.of(collaboratorSkill));
        when(quizGenerationClient.generateBlock(any())).thenReturn(blockPayload(3, "fallback"));
        when(assessmentSessionRepository.save(any(AssessmentSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QuizModels.AssessmentSessionState state = service.startSession(
            new AdaptiveAssessmentSessionService.StartSessionCommand(1L, 10L, StartLevelSource.HR, "Focus APIs")
        );

        assertThat(state.startingLevel()).isEqualTo(3);
        assertThat(state.currentLevel()).isEqualTo(3);
        assertThat(state.blocks()).hasSize(1);
        assertThat(state.blocks().getFirst().generationSource()).isEqualTo("fallback");
    }

    @Test
    void submitAnswersScoresBlockAndCreatesNextBlock() throws Exception {
        Collaborator collaborator = new Collaborator("Alice", "Martin", "alice@test.com", "Developer", "kc-alice");
        Skill skill = new Skill("Python", "Python skill", new SkillCategory("Backend", "Backend"));
        AssessmentSession session = new AssessmentSession(
            collaborator,
            skill,
            "Python",
            "python",
            StartLevelSource.HR,
            3,
            3,
            5,
            5,
            null,
            AssessmentSessionStatus.IN_PROGRESS,
            objectMapper.writeValueAsString(List.of(activeBlock(1, 3)))
        );

        when(assessmentSessionRepository.findById(42L)).thenReturn(Optional.of(session));
        when(assessmentSessionRepository.save(any(AssessmentSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(quizGenerationClient.generateBlock(any())).thenReturn(blockPayload(4, "llm"));

        QuizModels.AssessmentSessionState state = service.submitAnswers(
            42L,
            new AdaptiveAssessmentSessionService.SubmitAnswersCommand(
                1,
                List.of(
                    new QuizModels.SubmittedAnswer("q1", "A"),
                    new QuizModels.SubmittedAnswer("q2", "B"),
                    new QuizModels.SubmittedAnswer("q3", "C"),
                    new QuizModels.SubmittedAnswer("q4", "D"),
                    new QuizModels.SubmittedAnswer("q5", "A")
                )
            )
        );

        assertThat(state.currentLevel()).isEqualTo(4);
        assertThat(state.blocks()).hasSize(2);
        assertThat(state.blocks().get(0).scorePercentage()).isEqualTo(80);
        assertThat(state.blocks().get(0).transition()).isEqualTo(QuizModels.LevelTransition.UP);
        assertThat(state.blocks().get(1).level()).isEqualTo(4);
    }

    @Test
    void submitAnswersRejectsDuplicateQuestionIds() throws Exception {
        Collaborator collaborator = new Collaborator("Alice", "Martin", "alice@test.com", "Developer", "kc-alice");
        Skill skill = new Skill("Java", "Java skill", new SkillCategory("Backend", "Backend"));
        AssessmentSession session = new AssessmentSession(
            collaborator,
            skill,
            "Java",
            "java",
            StartLevelSource.SELF,
            2,
            2,
            5,
            5,
            null,
            AssessmentSessionStatus.IN_PROGRESS,
            objectMapper.writeValueAsString(List.of(activeBlock(1, 2)))
        );

        when(assessmentSessionRepository.findById(7L)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.submitAnswers(
            7L,
            new AdaptiveAssessmentSessionService.SubmitAnswersCommand(
                1,
                List.of(
                    new QuizModels.SubmittedAnswer("q1", "A"),
                    new QuizModels.SubmittedAnswer("q1", "B"),
                    new QuizModels.SubmittedAnswer("q3", "C"),
                    new QuizModels.SubmittedAnswer("q4", "D"),
                    new QuizModels.SubmittedAnswer("q5", "A")
                )
            )
        ))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("invalid-block-answers");
    }

    private QuizModels.GeneratedQuizBlock blockPayload(int level, String source) {
        return new QuizModels.GeneratedQuizBlock(
            "Quiz level " + level,
            List.of(
                new QuizModels.GeneratedQuestion("q1", "Topic 1", "Outcome 1", "Question 1", "A", "B", "C", "D", "Explanation 1"),
                new QuizModels.GeneratedQuestion("q2", "Topic 2", "Outcome 2", "Question 2", "A", "B", "C", "D", "Explanation 2"),
                new QuizModels.GeneratedQuestion("q3", "Topic 3", "Outcome 3", "Question 3", "A", "B", "C", "D", "Explanation 3"),
                new QuizModels.GeneratedQuestion("q4", "Topic 4", "Outcome 4", "Question 4", "A", "B", "C", "D", "Explanation 4"),
                new QuizModels.GeneratedQuestion("q5", "Topic 5", "Outcome 5", "Question 5", "A", "B", "C", "D", "Explanation 5")
            ),
            List.of(
                new QuizModels.ExpectedAnswer("q1", "A"),
                new QuizModels.ExpectedAnswer("q2", "B"),
                new QuizModels.ExpectedAnswer("q3", "C"),
                new QuizModels.ExpectedAnswer("q4", "D"),
                new QuizModels.ExpectedAnswer("q5", "B")
            ),
            new QuizModels.Difficulty(level, "Level " + level, "Description"),
            15,
            List.of("Criterion 1"),
            source,
            source.equals("fallback") ? "LLM unavailable" : null
        );
    }

    private QuizModels.SessionBlock activeBlock(int blockNumber, int level) {
        QuizModels.GeneratedQuizBlock block = blockPayload(level, "llm");
        return new QuizModels.SessionBlock(
            blockNumber,
            level,
            block.title(),
            block.questions(),
            block.expectedAnswers(),
            block.difficulty(),
            block.durationMinutes(),
            block.evaluationCriteria(),
            block.generationSource(),
            block.fallbackReason(),
            QuizModels.SessionBlockStatus.ACTIVE,
            List.of(),
            null,
            null,
            null
        );
    }
}
