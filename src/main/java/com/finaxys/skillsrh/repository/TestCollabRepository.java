package com.finaxys.skillsrh.repository;

import com.finaxys.skillsrh.domain.TestCollab;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TestCollabRepository extends JpaRepository<TestCollab, Long> {

    List<TestCollab> findByCollaborator_IdOrderByAssignedAtDesc(Long collaboratorId);

    Optional<TestCollab> findByIdAndCollaborator_Id(Long id, Long collaboratorId);
}
