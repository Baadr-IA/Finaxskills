package com.finaxys.skillsrh.repository;

import com.finaxys.skillsrh.domain.CollaboratorSkill;
import com.finaxys.skillsrh.domain.CollaboratorSkillId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CollaboratorSkillRepository extends JpaRepository<CollaboratorSkill, CollaboratorSkillId> {

    @Query("SELECT cs FROM CollaboratorSkill cs JOIN FETCH cs.skill s JOIN FETCH s.category WHERE cs.collaborator.id = :collaboratorId")
    List<CollaboratorSkill> findByCollaboratorId(@Param("collaboratorId") Long collaboratorId);
}
