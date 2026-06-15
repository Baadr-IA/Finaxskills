package com.finaxys.skillsrh.controller;

import com.finaxys.skillsrh.domain.Evaluation;
import com.finaxys.skillsrh.repository.EvaluationRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class EvaluationController {

    private final EvaluationRepository evaluationRepository;

    private static final DateTimeFormatter UI_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public EvaluationController(EvaluationRepository evaluationRepository) {
        this.evaluationRepository = evaluationRepository;
    }

    @GetMapping("/evaluations")
    @PreAuthorize("@permissions.has(authentication, 'COLLABORATORS', 'READ', 'ALL')")
    public List<EvaluationResponse> list() {
        List<Evaluation> evaluations = evaluationRepository.findAllByOrderByDateAssignedDesc();
        return evaluations.stream().map(this::toResponse).collect(Collectors.toList());
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
}


