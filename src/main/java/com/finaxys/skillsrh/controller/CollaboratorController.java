package com.finaxys.skillsrh.controller;

import com.finaxys.skillsrh.api.ApiException;
import com.finaxys.skillsrh.domain.Collaborator;
import com.finaxys.skillsrh.domain.CollaboratorSkill;
import com.finaxys.skillsrh.domain.Skill;
import com.finaxys.skillsrh.repository.CollaboratorRepository;
import com.finaxys.skillsrh.repository.CollaboratorSkillRepository;
import com.finaxys.skillsrh.repository.SkillRepository;
import com.finaxys.skillsrh.service.CollaboratorDeletionService;
import com.finaxys.skillsrh.service.KeycloakAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CollaboratorController {

    private final CollaboratorRepository collaboratorRepository;
    private final CollaboratorSkillRepository collaboratorSkillRepository;
    private final SkillRepository skillRepository;
    private final CollaboratorDeletionService collaboratorDeletionService;
    private final KeycloakAdminService keycloakAdminService;

    public CollaboratorController(
        CollaboratorRepository collaboratorRepository,
        CollaboratorSkillRepository collaboratorSkillRepository,
        SkillRepository skillRepository,
        CollaboratorDeletionService collaboratorDeletionService,
        KeycloakAdminService keycloakAdminService
    ) {
        this.collaboratorRepository = collaboratorRepository;
        this.collaboratorSkillRepository = collaboratorSkillRepository;
        this.skillRepository = skillRepository;
        this.collaboratorDeletionService = collaboratorDeletionService;
        this.keycloakAdminService = keycloakAdminService;
    }

    @GetMapping("/collaborators")
    @PreAuthorize("@permissions.has(authentication, 'COLLABORATORS', 'READ', 'ALL')")
    public List<CollaboratorResponse> list() {
        return collaboratorRepository.findAll().stream()
            .sorted((a, b) -> a.getLastName().compareToIgnoreCase(b.getLastName()))
            .map(this::toResponse)
            .toList();
    }

    @GetMapping("/collaborators/{id}")
    @PreAuthorize("@permissions.has(authentication, 'COLLABORATORS', 'READ', 'ALL')")
    public CollaboratorResponse getById(@PathVariable Long id) {
        return toResponse(require(id));
    }

    @PostMapping("/collaborators")
    @PreAuthorize("@permissions.has(authentication, 'COLLABORATORS', 'CREATE', 'ALL')")
    public ResponseEntity<CollaboratorResponse> create(Authentication authentication, @Valid @RequestBody CreateCollaboratorRequest req) {
        // create collaborator entity
        Collaborator c = new Collaborator(req.firstName(), req.lastName(), req.email(), req.jobTitle(), null);
        Skill declaredSkill = null;
        if (req.skillId() != null) {
            declaredSkill = skillRepository.findById(req.skillId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "skill-not-found", "Skill not found"));
        }

        // attempt to create Keycloak user using caller token if they have MANAGE_USERS role, otherwise fall back to admin credentials
        try {
            String kcId = null;
            if (authentication != null) {
                boolean hasManage = authentication.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_MANAGE_USERS".equals(a.getAuthority()));
                if (hasManage) {
                    // try to extract bearer token from JwtAuthenticationToken
                    try {
                        if (authentication instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken jwtAuth) {
                            String bearer = jwtAuth.getToken().getTokenValue();
                            kcId = keycloakAdminService.createUserIfNotExistsWithBearer(bearer, req.email(), req.firstName(), req.lastName());
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            // Use admin credentials to create user and set password equal to last name
            try {
                String pwd = req.lastName() != null ? req.lastName() : "changeme";
                System.out.println("Contenu du request: " + req);
                String id = keycloakAdminService.createUserAndSetPassword(req.email(), req.firstName(), req.lastName(), pwd);
                if (id != null) kcId = id;
            } catch (Exception ignored) {
            }

            if (kcId != null) {
                c.setKeycloakId(kcId);
            }
        } catch (Exception ignored) {
            // don't block collaborator creation if Keycloak fails; log could be added
        }

        Collaborator saved = save(c);
        if (declaredSkill != null && req.selfLevel() != null) {
            CollaboratorSkill collaboratorSkill = new CollaboratorSkill(saved, declaredSkill);
            collaboratorSkill.setSelfLevel(req.selfLevel());
            collaboratorSkillRepository.save(collaboratorSkill);
        }
        String kcMsg = null;
        if (saved.getKeycloakId() == null) {
            kcMsg = keycloakAdminService.getLastError();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved, kcMsg));
    }

    @PutMapping("/collaborators/{id}")
    @PreAuthorize("@permissions.has(authentication, 'COLLABORATORS', 'UPDATE', 'ALL')")
    public CollaboratorResponse update(@PathVariable Long id, @Valid @RequestBody UpdateCollaboratorRequest req) {
        Collaborator c = require(id);
        c.setFirstName(req.firstName());
        c.setLastName(req.lastName());
        c.setEmail(req.email());
        c.setJobTitle(req.jobTitle());
        c.setKeycloakId(req.keycloakId());
        return toResponse(save(c));
    }

    @DeleteMapping("/collaborators/{id}")
    @PreAuthorize("@permissions.has(authentication, 'COLLABORATORS', 'DELETE', 'ALL')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        collaboratorDeletionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private Collaborator require(Long id) {
        return collaboratorRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "collaborator-not-found", "Collaborator not found"));
    }

    private Collaborator save(Collaborator c) {
        try {
            return collaboratorRepository.save(c);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "collaborator-email-conflict", "A collaborator with this email already exists");
        }
    }

    private CollaboratorResponse toResponse(Collaborator c, String kcMessage) {
        return new CollaboratorResponse(c.getId(), c.getFirstName(), c.getLastName(), c.getEmail(), c.getJobTitle(), c.getKeycloakId(), kcMessage);
    }

    private CollaboratorResponse toResponse(Collaborator c) {
        return toResponse(c, null);
    }

    public record CollaboratorResponse(Long id, String firstName, String lastName, String email, String jobTitle, String keycloakId, String keycloakMessage) {}

    public record CreateCollaboratorRequest(
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @NotBlank @Email @Size(max = 255) String email,
            @Size(max = 150) String jobTitle,
            @NotNull Long skillId,
            @NotNull @Min(1) @Max(4) Integer selfLevel
        ) {}

    public record UpdateCollaboratorRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 150) String jobTitle,
        @Size(max = 255) String keycloakId
    ) {}
}
