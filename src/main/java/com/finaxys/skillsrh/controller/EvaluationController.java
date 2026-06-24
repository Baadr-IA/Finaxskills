package com.finaxys.skillsrh.controller;

import com.finaxys.skillsrh.api.ApiException;
import com.finaxys.skillsrh.domain.Collaborator;
import com.finaxys.skillsrh.domain.CollaboratorSkill;
import com.finaxys.skillsrh.domain.Evaluation;
import com.finaxys.skillsrh.domain.Skill;
import com.finaxys.skillsrh.domain.TestCollab;
import com.finaxys.skillsrh.domain.Status;
import com.finaxys.skillsrh.repository.CollaboratorRepository;
import com.finaxys.skillsrh.repository.CollaboratorSkillRepository;
import com.finaxys.skillsrh.repository.EvaluationRepository;
import com.finaxys.skillsrh.repository.SkillRepository;
import com.finaxys.skillsrh.repository.TestCollabRepository;
import com.finaxys.skillsrh.service.EvaluationQuizService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class EvaluationController {

    private static final Set<String> ALLOWED_EVALUATIONS = Set.of(
        "test fondamentaux java",
        "test fondamentaux python"
    );

    private final EvaluationRepository evaluationRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final CollaboratorSkillRepository collaboratorSkillRepository;
    private final SkillRepository skillRepository;
    private final TestCollabRepository testCollabRepository;
    private final EvaluationQuizService evaluationQuizService;

    private static final DateTimeFormatter UI_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public EvaluationController(
        EvaluationRepository evaluationRepository,
        CollaboratorRepository collaboratorRepository,
        CollaboratorSkillRepository collaboratorSkillRepository,
        SkillRepository skillRepository,
        TestCollabRepository testCollabRepository,
        EvaluationQuizService evaluationQuizService
    ) {
        this.evaluationRepository = evaluationRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.collaboratorSkillRepository = collaboratorSkillRepository;
        this.skillRepository = skillRepository;
        this.testCollabRepository = testCollabRepository;
        this.evaluationQuizService = evaluationQuizService;
    }

    @PostMapping("/evaluations")
    @PreAuthorize("@permissions.has(authentication, 'COLLABORATORS', 'CREATE', 'ALL')")
    public ResponseEntity<EvaluationResponse> create(@Valid @RequestBody CreateEvaluationRequest req) {
        Collaborator collaborator = collaboratorRepository.findById(req.collaboratorId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "collaborator-not-found", "Collaborator not found"));

        String normalized = req.evaluationName().trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_EVALUATIONS.contains(normalized)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "evaluation-not-in-referential", "Evaluation is not available in referential");
        }

        Evaluation evaluation = new Evaluation(collaborator, req.evaluationName().trim(), Status.EN_ATTENTE, LocalDate.now());
        evaluation.setLevelDeclared(resolveDeclaredLevel(collaborator, req.evaluationName().trim()));
        Evaluation saved = evaluationRepository.save(evaluation);

        // create a corresponding test_collab record to track assignment for the collaborator
        TestCollab tc = new TestCollab(saved, collaborator, Instant.now(), saved.getStatus(), 0, LocalDate.now().plusDays(7));
        testCollabRepository.save(tc);

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @GetMapping("/evaluations")
    @PreAuthorize("@permissions.has(authentication, 'COLLABORATORS', 'READ', 'ALL')")
    public List<EvaluationResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status
    ) {
        List<Evaluation> evaluations = evaluationRepository.findAllByOrderByDateAssignedDesc();

        // apply simple in-memory filtering for search and status
        return evaluations.stream()
                .filter(e -> {
                    if (q != null && !q.trim().isEmpty()) {
                        String nq = q.toLowerCase();
                        String collaborator = (e.getCollaborator().getFirstName() + " " + e.getCollaborator().getLastName()).toLowerCase();
                        String evalName = e.getEvaluationName() != null ? e.getEvaluationName().toLowerCase() : "";
                        String jobTitle = e.getCollaborator().getJobTitle() != null ? e.getCollaborator().getJobTitle().toLowerCase() : "";
                        if (!(collaborator.contains(nq) || evalName.contains(nq) || jobTitle.contains(nq))) {
                            return false;
                        }
                    }
                    if (status != null && !status.trim().isEmpty()) {
                        // map frontend status values to domain Status
                        try {
                            switch (status.trim().toLowerCase()) {
                                case "pending":
                                    return e.getStatus() == com.finaxys.skillsrh.domain.Status.EN_ATTENTE;
                                case "in_progress":
                                    return e.getStatus() == com.finaxys.skillsrh.domain.Status.EN_COURS;
                                case "completed":
                                    return e.getStatus() == com.finaxys.skillsrh.domain.Status.COMPLETE;
                                default:
                                    // try to match by label or name
                                    try {
                                        com.finaxys.skillsrh.domain.Status s = com.finaxys.skillsrh.domain.Status.fromLabel(status);
                                        return e.getStatus() == s;
                                    } catch (Exception ex) {
                                        return true; // unknown filter value -> do not filter out
                                    }
                            }
                        } catch (Exception ex) {
                            return true;
                        }
                    }
                    return true;
                })
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // collaborator-specific list: evaluations assigned to the authenticated user
    @GetMapping("/me/evaluations")
    public List<AssignedEvaluationResponse> myList(Authentication authentication,
                                                   @RequestParam(required = false) String q,
                                                   @RequestParam(required = false) String status) {
        Collaborator me = requireByKeycloakSubject(authentication);
        List<TestCollab> assignments = testCollabRepository.findByCollaborator_IdOrderByAssignedAtDesc(me.getId());

        return assignments.stream()
                .filter(e -> {
                    if (q != null && !q.trim().isEmpty()) {
                        String nq = q.toLowerCase();
                        String evalName = e.getEvaluation().getEvaluationName() != null ? e.getEvaluation().getEvaluationName().toLowerCase() : "";
                        if (!evalName.contains(nq)) {
                            return false;
                        }
                    }
                    if (status != null && !status.trim().isEmpty()) {
                        try {
                            switch (status.trim().toLowerCase()) {
                                case "pending":
                                    return normalizeStatus(e.getStatus()) == Status.EN_ATTENTE;
                                case "in_progress":
                                    return normalizeStatus(e.getStatus()) == Status.EN_COURS;
                                case "completed":
                                    return normalizeStatus(e.getStatus()) == Status.COMPLETE;
                                default:
                                    try {
                                        Status s = Status.fromLabel(status);
                                        return normalizeStatus(e.getStatus()) == s;
                                    } catch (Exception ex) {
                                        return true;
                                    }
                            }
                        } catch (Exception ex) {
                            return true;
                        }
                    }
                    return true;
                })
                .map(this::toAssignedResponse)
                .collect(Collectors.toList());
    }

    @PostMapping("/me/evaluations/{assignmentId}/start")
    public EvaluationQuizService.StartEvaluationResponse startMyEvaluation(
        Authentication authentication,
        @PathVariable Long assignmentId
    ) {
        Collaborator me = requireByKeycloakSubject(authentication);
        TestCollab assignment = testCollabRepository.findByIdAndCollaborator_Id(assignmentId, me.getId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "evaluation-assignment-not-found",
                "Evaluation assignment not found for current collaborator"));

        Skill skill = resolveSkillForEvaluation(assignment.getEvaluation().getEvaluationName());
        if (skill == null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "evaluation-skill-not-resolved",
                "Cannot resolve evaluated skill from evaluation label");
        }

        int declaredLevel = parseDeclaredLevel(assignment.getEvaluation().getLevelDeclared());
        return evaluationQuizService.start(assignmentId, me.getId(), skill, declaredLevel);
    }

    @PostMapping("/me/evaluations/{assignmentId}/answers")
    public EvaluationQuizService.SubmitEvaluationResponse submitMyEvaluationAnswers(
        Authentication authentication,
        @PathVariable Long assignmentId,
        @Valid @RequestBody SubmitEvaluationAnswersRequest request
    ) {
        Collaborator me = requireByKeycloakSubject(authentication);
        List<EvaluationQuizService.SubmitAnswerRequest> answers = request.answers().stream()
            .map(item -> new EvaluationQuizService.SubmitAnswerRequest(item.questionIndex(), item.optionCode()))
            .toList();
        return evaluationQuizService.submitAnswers(assignmentId, me.getId(), answers);
    }

    private EvaluationResponse toResponse(Evaluation e) {
        String collaborator = e.getCollaborator().getFirstName() + " " + e.getCollaborator().getLastName();
        String jobTitle = e.getCollaborator().getJobTitle();
        String dateAssigned = e.getDateAssigned() != null ? e.getDateAssigned().format(UI_DATE) : "N/A";
        String datePlanned = e.getDatePlanned() != null ? e.getDatePlanned().format(UI_DATE) : null;
        String dateValidated = e.getDateValidated() != null ? e.getDateValidated().format(UI_DATE) : null;

        return new EvaluationResponse(
            e.getId(),
            collaborator,
            jobTitle,
            e.getEvaluationName(),
            e.getStatus() != null ? e.getStatus().getLabel() : null,
            dateAssigned,
            datePlanned,
            dateValidated,
            e.getLevelDeclared(),
            e.getLevelValidated(),
            e.getScore()
        );
    }

    private Collaborator requireByKeycloakId(String keycloakId) {
        return collaboratorRepository.findByKeycloakId(keycloakId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "collaborator-not-found",
                "No collaborator profile linked to your account — contact your HR administrator"));
    }

    private Collaborator requireByKeycloakSubject(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            String subject = jwtAuthenticationToken.getToken().getSubject();
            if (subject != null && !subject.isBlank()) {
                return requireByKeycloakId(subject);
            }
        }
        return requireByKeycloakId(authentication.getName());
    }

    private Status normalizeStatus(Status status) {
        return status != null ? status : Status.EN_ATTENTE;
    }

    private Status normalizeCollaboratorStatus(Status status) {
        if (status == null || status == Status.EN_COURS) {
            return Status.EN_ATTENTE;
        }
        return status;
    }

    private AssignedEvaluationResponse toAssignedResponse(TestCollab tc) {
        String dateAssigned = tc.getAssignedAt() != null ? tc.getAssignedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(UI_DATE) : "N/A";
        String dueDate = tc.getDueDate() != null ? tc.getDueDate().format(UI_DATE) : null;
        Status status = normalizeCollaboratorStatus(tc.getStatus());
        Skill evaluatedSkill = resolveSkillForEvaluation(tc.getEvaluation().getEvaluationName());
        String competenceEvaluated = evaluatedSkill != null ? evaluatedSkill.getName() : "N/A";
        List<String> actions = new ArrayList<>();
        switch (status) {
            case EN_ATTENTE -> actions.add("start");
            case EN_COURS -> actions.add("resume");
            case COMPLETE -> actions.add("view");
        }
        return new AssignedEvaluationResponse(
            tc.getId(),
            tc.getEvaluation().getId(),
            tc.getEvaluation().getEvaluationName(),
            dateAssigned,
            status.getLabel(),
            competenceEvaluated,
            tc.getEvaluation().getLevelDeclared(),
            tc.getEvaluation().getLevelValidated(),
            tc.getProgress() != null ? tc.getProgress() : 0,
            tc.getEvaluation().getScore(),
            dueDate,
            actions
        );
    }

    private String resolveDeclaredLevel(Collaborator collaborator, String evaluationName) {
        Skill skill = resolveSkillForEvaluation(evaluationName);
        if (skill == null) {
            return null;
        }

        return collaboratorSkillRepository.findByCollaboratorId(collaborator.getId()).stream()
            .filter(cs -> cs.getSkill().getId().equals(skill.getId()))
            .map(CollaboratorSkill::getSelfLevel)
            .filter(level -> level != null && level >= 1 && level <= 4)
            .findFirst()
            .map(level -> "NIVEAU " + level)
            .orElse(null);
    }

    private Skill resolveSkillForEvaluation(String evaluationName) {
        String normalized = evaluationName == null ? "" : evaluationName.trim().toLowerCase(Locale.ROOT);
        for (Skill skill : skillRepository.findAll()) {
            String skillName = skill.getName() == null ? "" : skill.getName().trim().toLowerCase(Locale.ROOT);
            if (!skillName.isEmpty() && normalized.contains(skillName)) {
                return skill;
            }
        }
        if (normalized.contains("java")) {
            return skillRepository.findAll().stream()
                .filter(skill -> "java".equalsIgnoreCase(skill.getName()))
                .findFirst()
                .orElse(null);
        }
        if (normalized.contains("python")) {
            return skillRepository.findAll().stream()
                .filter(skill -> "python".equalsIgnoreCase(skill.getName()))
                .findFirst()
                .orElse(null);
        }
        return null;
    }

    private int parseDeclaredLevel(String levelDeclared) {
        if (levelDeclared == null || levelDeclared.isBlank()) {
            return 1;
        }
        String normalized = levelDeclared.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("4")) return 4;
        if (normalized.contains("3")) return 3;
        if (normalized.contains("2")) return 2;
        return 1;
    }

    public record EvaluationResponse(
        Long id,
        String collaborator,
        String jobTitle,
        String evaluation,
        String status,
        String dateAssigned,
        String datePlanned,
        String dateValidated,
        String levelDeclared,
        String levelValidated,
        String score
    ) {}

    public record AssignedEvaluationResponse(
        Long id,
        Long evaluationId,
        String evaluation,
        String dateAssigned,
        String status,
        String competenceEvaluated,
        String levelDeclared,
        String levelValidated,
        Integer progress,
        String score,
        String dueDate,
        List<String> availableActions
    ) {}

    public record CreateEvaluationRequest(
        @NotNull Long collaboratorId,
        @NotBlank @Size(max = 255) String evaluationName
    ) {}

    public record SubmitEvaluationAnswersRequest(
        @NotNull List<@Valid SubmitAnswerItemRequest> answers
    ) {}

    public record SubmitAnswerItemRequest(
        @NotNull Integer questionIndex,
        @NotBlank String optionCode
    ) {}
}
