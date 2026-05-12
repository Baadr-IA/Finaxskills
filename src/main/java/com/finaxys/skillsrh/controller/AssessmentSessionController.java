package com.finaxys.skillsrh.controller;

import com.finaxys.skillsrh.domain.StartLevelSource;
import com.finaxys.skillsrh.service.assessment.AdaptiveAssessmentSessionService;
import com.finaxys.skillsrh.service.assessment.QuizModels;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/assessment-sessions")
public class AssessmentSessionController {

    private final AdaptiveAssessmentSessionService assessmentSessionService;

    public AssessmentSessionController(AdaptiveAssessmentSessionService assessmentSessionService) {
        this.assessmentSessionService = assessmentSessionService;
    }

    @PostMapping
    @PreAuthorize("@permissions.has(authentication, 'SKILL_ASSESSMENTS', 'CREATE', 'ALL')")
    public ResponseEntity<AssessmentSessionResponse> startSession(@Valid @RequestBody StartSessionRequest request) {
        QuizModels.AssessmentSessionState session = assessmentSessionService.startSession(
            new AdaptiveAssessmentSessionService.StartSessionCommand(
                request.collaboratorId(),
                request.skillId(),
                request.startLevelSource(),
                request.instructions()
            )
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(session));
    }

    @GetMapping("/{sessionId}")
    @PreAuthorize("@permissions.has(authentication, 'SKILL_ASSESSMENTS', 'READ', 'ALL')")
    public AssessmentSessionResponse getSession(@PathVariable Long sessionId) {
        return toResponse(assessmentSessionService.getSession(sessionId));
    }

    @PostMapping("/{sessionId}/blocks/{blockNumber}/answers")
    @PreAuthorize("@permissions.has(authentication, 'SKILL_ASSESSMENTS', 'UPDATE', 'ALL')")
    public AssessmentSessionResponse submitAnswers(
        @PathVariable Long sessionId,
        @PathVariable int blockNumber,
        @Valid @RequestBody SubmitAnswersRequest request
    ) {
        QuizModels.AssessmentSessionState session = assessmentSessionService.submitAnswers(
            sessionId,
            new AdaptiveAssessmentSessionService.SubmitAnswersCommand(blockNumber, request.answers())
        );
        return toResponse(session);
    }

    private AssessmentSessionResponse toResponse(QuizModels.AssessmentSessionState session) {
        return new AssessmentSessionResponse(
            session.id(),
            session.collaboratorId(),
            session.collaboratorName(),
            session.skillId(),
            session.skillName(),
            session.skillKey(),
            session.startLevelSource(),
            session.startingLevel(),
            session.currentLevel(),
            session.totalBlocks(),
            session.questionCountPerBlock(),
            session.generationInstructions(),
            session.status().name(),
            session.createdAt(),
            session.updatedAt(),
            session.blocks().stream().map(this::toBlockResponse).toList()
        );
    }

    private AssessmentBlockResponse toBlockResponse(QuizModels.SessionBlock block) {
        return new AssessmentBlockResponse(
            block.blockNumber(),
            block.level(),
            block.title(),
            block.difficulty(),
            block.durationMinutes(),
            block.evaluationCriteria(),
            block.generationSource(),
            block.fallbackReason(),
            block.status().name(),
            block.questions().stream().map(this::toQuestionResponse).toList(),
            block.submittedAnswers(),
            block.correctAnswers(),
            block.scorePercentage(),
            block.transition() == null ? null : block.transition().name()
        );
    }

    private QuestionResponse toQuestionResponse(QuizModels.GeneratedQuestion question) {
        return new QuestionResponse(
            question.id(),
            question.topic(),
            question.targetedOutcome(),
            question.text(),
            question.optionA(),
            question.optionB(),
            question.optionC(),
            question.optionD()
        );
    }

    public record StartSessionRequest(
        @NotNull Long collaboratorId,
        @NotNull Long skillId,
        @NotNull StartLevelSource startLevelSource,
        @Size(max = 1000) String instructions
    ) {}

    public record SubmitAnswersRequest(
        @NotEmpty List<QuizModels.SubmittedAnswer> answers
    ) {}

    public record AssessmentSessionResponse(
        Long id,
        Long collaboratorId,
        String collaboratorName,
        Long skillId,
        String skillName,
        String skillKey,
        StartLevelSource startLevelSource,
        Integer startingLevel,
        Integer currentLevel,
        Integer totalBlocks,
        Integer questionCountPerBlock,
        String generationInstructions,
        String status,
        Instant createdAt,
        Instant updatedAt,
        List<AssessmentBlockResponse> blocks
    ) {}

    public record AssessmentBlockResponse(
        Integer blockNumber,
        Integer level,
        String title,
        QuizModels.Difficulty difficulty,
        Integer durationMinutes,
        List<String> evaluationCriteria,
        String generationSource,
        String fallbackReason,
        String status,
        List<QuestionResponse> questions,
        List<QuizModels.SubmittedAnswer> submittedAnswers,
        Integer correctAnswers,
        Integer scorePercentage,
        String transition
    ) {}

    public record QuestionResponse(
        String id,
        String topic,
        String targetedOutcome,
        String text,
        String optionA,
        String optionB,
        String optionC,
        String optionD
    ) {}
}
