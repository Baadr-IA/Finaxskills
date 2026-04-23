package com.finaxys.skillsrh.repository;

import com.finaxys.skillsrh.domain.Collaborator;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CollaboratorRepository extends JpaRepository<Collaborator, Long> {

    Optional<Collaborator> findByEmail(String email);

    Optional<Collaborator> findByKeycloakId(String keycloakId);
}
