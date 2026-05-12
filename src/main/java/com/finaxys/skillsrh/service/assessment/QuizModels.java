package com.finaxys.skillsrh.service.assessment;

import com.finaxys.skillsrh.domain.AssessmentSessionStatus;
import com.finaxys.skillsrh.domain.StartLevelSource;

import java.time.Instant;
import java.util.List;

public final class QuizModels {

    private QuizModels() {
    }

    public record QuizGenerationRequest(
        String skill,
        int level,
        int questionCount,
        String instructions
    ) {}

    public record Difficulty(
        int level,
        String label,
        String description
    ) {}

    public record GeneratedQuestion(
        String id,
        String topic,
        String targetedOutcome,
        String text,
        String optionA,
        String optionB,
        String optionC,
        String optionD,
        String explanation
    ) {}

    public record ExpectedAnswer(
        String questionId,
        String option
    ) {}

    public record SubmittedAnswer(
        String questionId,
        String option
    ) {}

    public record GeneratedQuizBlock(
        String title,
        List<GeneratedQuestion> questions,
        List<ExpectedAnswer> expectedAnswers,
        Difficulty difficulty,
        int durationMinutes,
        List<String> evaluationCriteria,
        String generationSource,
        String fallbackReason
    ) {}

    public enum SessionBlockStatus {
        ACTIVE,
        ANSWERED
    }

    public enum LevelTransition {
        UP,
        STAY,
        DOWN
    }

    public record SessionBlock(
        int blockNumber,
        int level,
        String title,
        List<GeneratedQuestion> questions,
        List<ExpectedAnswer> expectedAnswers,
        Difficulty difficulty,
        int durationMinutes,
        List<String> evaluationCriteria,
        String generationSource,
        String fallbackReason,
        SessionBlockStatus status,
        List<SubmittedAnswer> submittedAnswers,
        Integer correctAnswers,
        Integer scorePercentage,
        LevelTransition transition
    ) {}

    public record AssessmentSessionState(
        Long id,
        Long collaboratorId,
        String collaboratorName,
        Long skillId,
        String skillName,
        String skillKey,
        StartLevelSource startLevelSource,
        int startingLevel,
        int currentLevel,
        int totalBlocks,
        int questionCountPerBlock,
        String generationInstructions,
        AssessmentSessionStatus status,
        Instant createdAt,
        Instant updatedAt,
        List<SessionBlock> blocks
    ) {}
}
