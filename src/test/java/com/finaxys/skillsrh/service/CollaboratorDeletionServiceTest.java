package com.finaxys.skillsrh.service;

import com.finaxys.skillsrh.api.ApiException;
import com.finaxys.skillsrh.domain.Collaborator;
import com.finaxys.skillsrh.repository.AnswerOptionRepository;
import com.finaxys.skillsrh.repository.AssessmentSessionRepository;
import com.finaxys.skillsrh.repository.CollaboratorAnswerRepository;
import com.finaxys.skillsrh.repository.CollaboratorRepository;
import com.finaxys.skillsrh.repository.CollaboratorSkillRepository;
import com.finaxys.skillsrh.repository.EvaluationRepository;
import com.finaxys.skillsrh.repository.QuestionRepository;
import com.finaxys.skillsrh.repository.TestCollabRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollaboratorDeletionServiceTest {

    @Mock
    private CollaboratorRepository collaboratorRepository;

    @Mock
    private CollaboratorAnswerRepository collaboratorAnswerRepository;

    @Mock
    private AnswerOptionRepository answerOptionRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private TestCollabRepository testCollabRepository;

    @Mock
    private AssessmentSessionRepository assessmentSessionRepository;

    @Mock
    private CollaboratorSkillRepository collaboratorSkillRepository;

    @Mock
    private EvaluationRepository evaluationRepository;

    private CollaboratorDeletionService service;

    @BeforeEach
    void setUp() {
        service = new CollaboratorDeletionService(
            collaboratorRepository,
            collaboratorAnswerRepository,
            answerOptionRepository,
            questionRepository,
            testCollabRepository,
            assessmentSessionRepository,
            collaboratorSkillRepository,
            evaluationRepository
        );
    }

    @Test
    void deleteByIdDeletesDependenciesBeforeCollaborator() {
        Collaborator collaborator = new Collaborator("Alice", "Martin", "alice@test.com", "Developer", "kc-alice");
        when(collaboratorRepository.findById(7L)).thenReturn(Optional.of(collaborator));

        service.deleteById(7L);

        InOrder inOrder = inOrder(
            collaboratorAnswerRepository,
            answerOptionRepository,
            questionRepository,
            testCollabRepository,
            assessmentSessionRepository,
            collaboratorSkillRepository,
            evaluationRepository,
            collaboratorRepository
        );

        inOrder.verify(collaboratorAnswerRepository).deleteByQuestion_TestCollab_Collaborator_Id(7L);
        inOrder.verify(collaboratorAnswerRepository).deleteByCollaborator_Id(7L);
        inOrder.verify(answerOptionRepository).deleteByQuestion_TestCollab_Collaborator_Id(7L);
        inOrder.verify(questionRepository).deleteByTestCollab_Collaborator_Id(7L);
        inOrder.verify(testCollabRepository).deleteByCollaborator_Id(7L);
        inOrder.verify(assessmentSessionRepository).deleteByCollaborator_Id(7L);
        inOrder.verify(collaboratorSkillRepository).deleteByCollaborator_Id(7L);
        inOrder.verify(evaluationRepository).deleteByCollaborator_Id(7L);
        inOrder.verify(collaboratorRepository).delete(collaborator);
        inOrder.verify(collaboratorRepository).flush();
    }

    @Test
    void deleteByIdReturns404WhenCollaboratorDoesNotExist() {
        when(collaboratorRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteById(42L))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("collaborator-not-found");

        verifyNoInteractions(
            collaboratorAnswerRepository,
            answerOptionRepository,
            questionRepository,
            testCollabRepository,
            assessmentSessionRepository,
            collaboratorSkillRepository,
            evaluationRepository
        );
    }

    @Test
    void deleteByIdReturns409WhenAConstraintStillBlocksDeletion() {
        Collaborator collaborator = new Collaborator("Alice", "Martin", "alice@test.com", "Developer", "kc-alice");
        when(collaboratorRepository.findById(7L)).thenReturn(Optional.of(collaborator));
        org.mockito.Mockito.doThrow(new DataIntegrityViolationException("fk violation"))
            .when(collaboratorRepository)
            .flush();

        assertThatThrownBy(() -> service.deleteById(7L))
            .isInstanceOf(ApiException.class)
            .matches(ex -> ((ApiException) ex).getStatusCode().value() == HttpStatus.CONFLICT.value())
            .hasMessageContaining("collaborator-delete-conflict");
    }
}

