package com.finaxys.skillsrh.controller;

import com.finaxys.skillsrh.api.ApiException;
import com.finaxys.skillsrh.domain.Collaborator;
import com.finaxys.skillsrh.domain.CollaboratorSkill;
import com.finaxys.skillsrh.domain.CollaboratorSkillId;
import com.finaxys.skillsrh.domain.Skill;
import com.finaxys.skillsrh.repository.CollaboratorRepository;
import com.finaxys.skillsrh.repository.CollaboratorSkillRepository;
import com.finaxys.skillsrh.repository.SkillRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CollaboratorSkillController {

    private final CollaboratorRepository collaboratorRepository;
    private final SkillRepository skillRepository;
    private final CollaboratorSkillRepository assessmentRepository;

    public CollaboratorSkillController(
        CollaboratorRepository collaboratorRepository,
        SkillRepository skillRepository,
        CollaboratorSkillRepository assessmentRepository
    ) {
        this.collaboratorRepository = collaboratorRepository;
        this.skillRepository = skillRepository;
        this.assessmentRepository = assessmentRepository;
    }

    // ── HR view: all assessments for a collaborator ──────────────────────────

    @GetMapping("/collaborators/{collaboratorId}/skills")
    @PreAuthorize("@permissions.has(authentication, 'SKILL_ASSESSMENTS', 'READ', 'ALL')")
    public List<AssessmentResponse> listForCollaborator(@PathVariable Long collaboratorId) {
        requireCollaborator(collaboratorId);
        return assessmentRepository.findByCollaboratorId(collaboratorId).stream()
            .map(this::toResponse)
            .toList();
    }

    @PutMapping("/collaborators/{collaboratorId}/skills/{skillId}")
    @PreAuthorize("@permissions.has(authentication, 'SKILL_ASSESSMENTS', 'UPDATE', 'ALL')")
    public AssessmentResponse upsertForCollaborator(
        @PathVariable Long collaboratorId,
        @PathVariable Long skillId,
        @Valid @RequestBody HrAssessmentRequest req
    ) {
        CollaboratorSkill assessment = getOrCreate(collaboratorId, skillId);
        assessment.setHrLevel(req.hrLevel());
        assessment.setHrNote(req.hrNote());
        assessment.setUpdatedAt(Instant.now());
        return toResponse(assessmentRepository.save(assessment));
    }

    @DeleteMapping("/collaborators/{collaboratorId}/skills/{skillId}")
    @PreAuthorize("@permissions.has(authentication, 'SKILL_ASSESSMENTS', 'DELETE', 'ALL')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteForCollaborator(@PathVariable Long collaboratorId, @PathVariable Long skillId) {
        CollaboratorSkillId id = new CollaboratorSkillId(collaboratorId, skillId);
        assessmentRepository.findById(id).ifPresent(assessmentRepository::delete);
    }

    // ── Collaborator self-evaluation: /me/skills ─────────────────────────────

    @GetMapping("/me/skills")
    @PreAuthorize("@permissions.has(authentication, 'SKILL_ASSESSMENTS', 'READ', 'SELF')")
    public List<AssessmentResponse> mySkills(Authentication authentication) {
        Collaborator me = requireByKeycloakId(authentication.getName());
        return assessmentRepository.findByCollaboratorId(me.getId()).stream()
            .map(this::toResponse)
            .toList();
    }

    @PutMapping("/me/skills/{skillId}")
    @PreAuthorize("@permissions.has(authentication, 'SKILL_ASSESSMENTS', 'UPDATE', 'SELF')")
    public AssessmentResponse selfEvaluate(
        Authentication authentication,
        @PathVariable Long skillId,
        @Valid @RequestBody SelfAssessmentRequest req
    ) {
        Collaborator me = requireByKeycloakId(authentication.getName());
        CollaboratorSkill assessment = getOrCreate(me.getId(), skillId);
        assessment.setSelfLevel(req.selfLevel());
        assessment.setSelfNote(req.selfNote());
        assessment.setUpdatedAt(Instant.now());
        return toResponse(assessmentRepository.save(assessment));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Collaborator requireCollaborator(Long id) {
        return collaboratorRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "collaborator-not-found", "Collaborator not found"));
    }

    private Collaborator requireByKeycloakId(String keycloakId) {
        return collaboratorRepository.findByKeycloakId(keycloakId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "collaborator-not-found",
                "No collaborator profile linked to your account — contact your HR administrator"));
    }

    private Skill requireSkill(Long id) {
        return skillRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "skill-not-found", "Skill not found"));
    }

    private CollaboratorSkill getOrCreate(Long collaboratorId, Long skillId) {
        CollaboratorSkillId id = new CollaboratorSkillId(collaboratorId, skillId);
        return assessmentRepository.findById(id).orElseGet(() -> {
            Collaborator collaborator = requireCollaborator(collaboratorId);
            Skill skill = requireSkill(skillId);
            return new CollaboratorSkill(collaborator, skill);
        });
    }

    private AssessmentResponse toResponse(CollaboratorSkill cs) {
        return new AssessmentResponse(
            cs.getCollaborator().getId(),
            cs.getSkill().getId(),
            cs.getSkill().getName(),
            cs.getSkill().getCategory().getId(),
            cs.getSkill().getCategory().getName(),
            cs.getSelfLevel(),
            cs.getSelfNote(),
            cs.getHrLevel(),
            cs.getHrNote(),
            cs.getUpdatedAt()
        );
    }

    public record AssessmentResponse(
        Long collaboratorId,
        Long skillId,
        String skillName,
        Long categoryId,
        String categoryName,
        Integer selfLevel,
        String selfNote,
        Integer hrLevel,
        String hrNote,
        java.time.Instant updatedAt
    ) {}

    public record SelfAssessmentRequest(
        @Min(1) @Max(5) Integer selfLevel,
        @Size(max = 500) String selfNote
    ) {}

    public record HrAssessmentRequest(
        @Min(1) @Max(5) Integer hrLevel,
        @Size(max = 500) String hrNote
    ) {}
}
