package com.finaxys.skillsrh.service;

import com.finaxys.skillsrh.domain.Collaborator;
import com.finaxys.skillsrh.repository.CollaboratorRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CollaboratorProvisioningService {

    private final CollaboratorRepository collaboratorRepository;

    public CollaboratorProvisioningService(CollaboratorRepository collaboratorRepository) {
        this.collaboratorRepository = collaboratorRepository;
    }

    @Transactional
    public Collaborator ensureCollaborator(Authentication authentication) {
        String keycloakId = resolveKeycloakId(authentication);

        return collaboratorRepository.findByKeycloakId(keycloakId)
            .orElseGet(() -> createCollaborator(authentication, keycloakId));
    }

    private Collaborator createCollaborator(Authentication authentication, String keycloakId) {
        Jwt jwt = extractJwt(authentication);

        String firstName = firstText(
            jwt.getClaimAsString("given_name"),
            jwt.getClaimAsString("preferred_username"),
            keycloakId
        );
        String lastName = firstText(jwt.getClaimAsString("family_name"), "Keycloak");
        String email = firstText(jwt.getClaimAsString("email"), keycloakId + "@keycloak.local");
        String jobTitle = jwt.getClaimAsString("job_title");

        Collaborator collaborator = new Collaborator(firstName, lastName, email, jobTitle, keycloakId);
        try {
            return collaboratorRepository.save(collaborator);
        } catch (DataIntegrityViolationException ex) {
            return collaboratorRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> ex);
        }
    }

    private String resolveKeycloakId(Authentication authentication) {
        Jwt jwt = extractJwt(authentication);
        String subject = jwt.getSubject();
        if (!StringUtils.hasText(subject)) {
            throw new AuthenticationCredentialsNotFoundException("Keycloak subject is required");
        }
        return subject;
    }

    private Jwt extractJwt(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return jwtAuthenticationToken.getToken();
        }

        throw new AuthenticationCredentialsNotFoundException("Keycloak JWT authentication is required");
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
