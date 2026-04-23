package com.finaxys.skillsrh.repository;

import com.finaxys.skillsrh.domain.Collaborator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class CollaboratorRepositoryTest {

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(POSTGRES_IMAGE);

    @Autowired
    private CollaboratorRepository collaboratorRepository;

    @BeforeEach
    void setUp() {
        collaboratorRepository.deleteAll();
    }

    @Test
    void findByEmailReturnsCollaboratorWhenEmailExists() {
        collaboratorRepository.saveAndFlush(
            new Collaborator("Alice", "Martin", "alice@test.com", "Dev", "kc-alice-001")
        );

        Optional<Collaborator> result = collaboratorRepository.findByEmail("alice@test.com");

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getFirstName()).isEqualTo("Alice");
        assertThat(result.orElseThrow().getLastName()).isEqualTo("Martin");
    }

    @Test
    void findByEmailReturnsEmptyWhenEmailDoesNotExist() {
        Optional<Collaborator> result = collaboratorRepository.findByEmail("nobody@test.com");

        assertThat(result).isEmpty();
    }

    @Test
    void findByKeycloakIdReturnsCollaboratorWhenIdExists() {
        collaboratorRepository.saveAndFlush(
            new Collaborator("Bob", "Dupont", "bob@test.com", "Dev", "kc-bob-007")
        );

        Optional<Collaborator> result = collaboratorRepository.findByKeycloakId("kc-bob-007");

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getEmail()).isEqualTo("bob@test.com");
    }

    @Test
    void findByKeycloakIdReturnsEmptyWhenIdDoesNotExist() {
        Optional<Collaborator> result = collaboratorRepository.findByKeycloakId("kc-unknown");

        assertThat(result).isEmpty();
    }

    @Test
    void saveRejectsDuplicateEmail() {
        collaboratorRepository.saveAndFlush(
            new Collaborator("Alice", "Martin", "alice@test.com", null, "kc-alice-001")
        );

        assertThatThrownBy(() ->
            collaboratorRepository.saveAndFlush(
                new Collaborator("Alice2", "Martin2", "alice@test.com", null, "kc-alice-002")
            )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void saveRejectsDuplicateKeycloakId() {
        collaboratorRepository.saveAndFlush(
            new Collaborator("Alice", "Martin", "alice@test.com", null, "kc-shared")
        );

        assertThatThrownBy(() ->
            collaboratorRepository.saveAndFlush(
                new Collaborator("Bob", "Dupont", "bob@test.com", null, "kc-shared")
            )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }
}
