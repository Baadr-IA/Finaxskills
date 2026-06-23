package com.finaxys.skillsrh.service;

import com.finaxys.skillsrh.domain.Collaborator;
import com.finaxys.skillsrh.repository.CollaboratorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollaboratorProvisioningServiceTest {

    @Mock
    private CollaboratorRepository collaboratorRepository;

    @InjectMocks
    private CollaboratorProvisioningService collaboratorProvisioningService;

    @Test
    void ensureCollaboratorReturnsExistingCollaboratorWhenFound() {
        JwtAuthenticationToken authentication = authentication("kc-user-123");
        Collaborator collaborator = new Collaborator("Alice", "Martin", "alice@test.com", "Dev", "kc-user-123");

        when(collaboratorRepository.findByKeycloakId("kc-user-123")).thenReturn(Optional.of(collaborator));

        Collaborator result = collaboratorProvisioningService.ensureCollaborator(authentication);

        assertThat(result).isSameAs(collaborator);
        verify(collaboratorRepository, never()).save(any());
    }

    @Test
    void ensureCollaboratorCreatesCollaboratorWhenMissing() {
        JwtAuthenticationToken authentication = authentication("kc-user-456");
        when(collaboratorRepository.findByKeycloakId("kc-user-456")).thenReturn(Optional.empty());
        when(collaboratorRepository.save(any(Collaborator.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Collaborator result = collaboratorProvisioningService.ensureCollaborator(authentication);

        ArgumentCaptor<Collaborator> collaboratorCaptor = ArgumentCaptor.forClass(Collaborator.class);
        verify(collaboratorRepository).save(collaboratorCaptor.capture());

        Collaborator saved = collaboratorCaptor.getValue();
        assertThat(saved.getKeycloakId()).isEqualTo("kc-user-456");
        assertThat(saved.getFirstName()).isEqualTo("Alice");
        assertThat(saved.getLastName()).isEqualTo("Martin");
        assertThat(saved.getEmail()).isEqualTo("alice.martin@finaxys.com");
        assertThat(result).isSameAs(saved);
    }

    @Test
    void ensureCollaboratorRejectsNonJwtAuthentication() {
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> collaboratorProvisioningService.ensureCollaborator(null));
    }

    private JwtAuthenticationToken authentication(String subject) {
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject(subject)
            .claim("preferred_username", "alice")
            .claim("given_name", "Alice")
            .claim("family_name", "Martin")
            .claim("email", "alice.martin@finaxys.com")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
        return new JwtAuthenticationToken(jwt);
    }
}
