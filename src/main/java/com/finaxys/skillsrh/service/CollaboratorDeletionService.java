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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CollaboratorDeletionService {

    private final CollaboratorRepository collaboratorRepository;
    private final CollaboratorAnswerRepository collaboratorAnswerRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final QuestionRepository questionRepository;
    private final TestCollabRepository testCollabRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final CollaboratorSkillRepository collaboratorSkillRepository;
    private final EvaluationRepository evaluationRepository;

    public CollaboratorDeletionService(
        CollaboratorRepository collaboratorRepository,
        CollaboratorAnswerRepository collaboratorAnswerRepository,
        AnswerOptionRepository answerOptionRepository,
        QuestionRepository questionRepository,
        TestCollabRepository testCollabRepository,
        AssessmentSessionRepository assessmentSessionRepository,
        CollaboratorSkillRepository collaboratorSkillRepository,
        EvaluationRepository evaluationRepository
    ) {
        this.collaboratorRepository = collaboratorRepository;
        this.collaboratorAnswerRepository = collaboratorAnswerRepository;
        this.answerOptionRepository = answerOptionRepository;
        this.questionRepository = questionRepository;
        this.testCollabRepository = testCollabRepository;
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.collaboratorSkillRepository = collaboratorSkillRepository;
        this.evaluationRepository = evaluationRepository;
    }

    @Transactional
    public void deleteById(Long collaboratorId) {
        Collaborator collaborator = collaboratorRepository.findById(collaboratorId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "collaborator-not-found", "Collaborator not found"));

        try {
            collaboratorAnswerRepository.deleteByQuestion_TestCollab_Collaborator_Id(collaboratorId);
            collaboratorAnswerRepository.deleteByCollaborator_Id(collaboratorId);
            answerOptionRepository.deleteByQuestion_TestCollab_Collaborator_Id(collaboratorId);
            questionRepository.deleteByTestCollab_Collaborator_Id(collaboratorId);
            testCollabRepository.deleteByCollaborator_Id(collaboratorId);
            assessmentSessionRepository.deleteByCollaborator_Id(collaboratorId);
            collaboratorSkillRepository.deleteByCollaborator_Id(collaboratorId);
            evaluationRepository.deleteByCollaborator_Id(collaboratorId);
            collaboratorRepository.delete(collaborator);
            collaboratorRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(
                HttpStatus.CONFLICT,
                "collaborator-delete-conflict",
                "Impossible de supprimer ce collaborateur car des données liées existent encore."
            );
        }
    }
}

