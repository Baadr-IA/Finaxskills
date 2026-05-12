package com.finaxys.skillsrh.repository;

import com.finaxys.skillsrh.domain.AssessmentSession;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AssessmentSessionRepository extends JpaRepository<AssessmentSession, Long> {

    @Override
    @EntityGraph(attributePaths = {"collaborator", "skill", "skill.category"})
    Optional<AssessmentSession> findById(Long id);
}
