package com.finaxys.skillsrh.repository;

import com.finaxys.skillsrh.domain.TestCollab;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestCollabRepository extends JpaRepository<TestCollab, Long> {

    List<TestCollab> findByCollaborator_IdOrderByAssignedAtDesc(Long collaboratorId);
}
