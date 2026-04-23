package com.finaxys.skillsrh.repository;

import com.finaxys.skillsrh.domain.Greeting;
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
class GreetingRepositoryTest {

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(POSTGRES_IMAGE);

    @Autowired
    private GreetingRepository greetingRepository;

    @BeforeEach
    void setUp() {
        greetingRepository.deleteAll();
    }

    @Test
    void findByKeyReturnsGreetingWhenKeyExists() {
        // Given
        greetingRepository.saveAndFlush(new Greeting("hello", "Hello World from Spring Boot"));

        // When
        Optional<Greeting> result = greetingRepository.findByKey("hello");

        // Then
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getMessage()).isEqualTo("Hello World from Spring Boot");
    }

    @Test
    void findByKeyReturnsEmptyWhenKeyDoesNotExist() {
        // Given
        greetingRepository.saveAndFlush(new Greeting("hello", "Hello World from Spring Boot"));

        // When
        Optional<Greeting> result = greetingRepository.findByKey("unknown-key");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void saveRejectsDuplicateGreetingKey() {
        // Given
        greetingRepository.saveAndFlush(new Greeting("hello", "First message"));

        // When / Then
        assertThatThrownBy(() -> greetingRepository.saveAndFlush(new Greeting("hello", "Second message")))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
}
