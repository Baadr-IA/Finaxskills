package com.finaxys.skillsrh.repository;

import com.finaxys.skillsrh.domain.CollaboratorAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollaboratorAnswerRepository extends JpaRepository<CollaboratorAnswer, Long> {

    void deleteByCollaborator_Id(Long collaboratorId);

    void deleteByQuestion_TestCollab_Collaborator_Id(Long collaboratorId);

    void deleteByCollaborator_IdAndQuestion_TestCollab_Id(Long collaboratorId, Long testCollabId);
}
