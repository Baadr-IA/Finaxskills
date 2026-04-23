package com.finaxys.skillsrh.controller;

import com.finaxys.skillsrh.api.ApiException;
import com.finaxys.skillsrh.domain.Collaborator;
import com.finaxys.skillsrh.repository.CollaboratorRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    public CollaboratorController(CollaboratorRepository collaboratorRepository) {
        this.collaboratorRepository = collaboratorRepository;
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
    public ResponseEntity<CollaboratorResponse> create(@Valid @RequestBody CreateCollaboratorRequest req) {
        Collaborator c = new Collaborator(req.firstName(), req.lastName(), req.email(), req.jobTitle(), req.keycloakId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(save(c)));
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
        collaboratorRepository.findById(id).ifPresent(collaboratorRepository::delete);
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

    private CollaboratorResponse toResponse(Collaborator c) {
        return new CollaboratorResponse(c.getId(), c.getFirstName(), c.getLastName(), c.getEmail(), c.getJobTitle(), c.getKeycloakId());
    }

    public record CollaboratorResponse(Long id, String firstName, String lastName, String email, String jobTitle, String keycloakId) {}

    public record CreateCollaboratorRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 150) String jobTitle,
        @Size(max = 255) String keycloakId
    ) {}

    public record UpdateCollaboratorRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 150) String jobTitle,
        @Size(max = 255) String keycloakId
    ) {}
}
