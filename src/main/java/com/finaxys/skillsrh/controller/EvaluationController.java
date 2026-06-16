package com.finaxys.skillsrh.controller;

import com.finaxys.skillsrh.api.ApiException;
import com.finaxys.skillsrh.domain.Collaborator;
import com.finaxys.skillsrh.domain.Evaluation;
import com.finaxys.skillsrh.domain.Status;
import com.finaxys.skillsrh.repository.CollaboratorRepository;
import com.finaxys.skillsrh.repository.EvaluationRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
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

    private static final DateTimeFormatter UI_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public EvaluationController(EvaluationRepository evaluationRepository, CollaboratorRepository collaboratorRepository) {
        this.evaluationRepository = evaluationRepository;
        this.collaboratorRepository = collaboratorRepository;
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
        Evaluation saved = evaluationRepository.save(evaluation);
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

    public record CreateEvaluationRequest(
        @NotNull Long collaboratorId,
        @NotBlank @Size(max = 255) String evaluationName
    ) {}
}


