package com.finaxys.skillsrh.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "assessment_session")
public class AssessmentSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "collaborator_id", nullable = false)
    private Collaborator collaborator;

    @ManyToOne(optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(name = "skill_name_snapshot", length = 100, nullable = false)
    private String skillNameSnapshot;

    @Column(name = "skill_key", length = 100, nullable = false)
    private String skillKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "start_level_source", length = 20, nullable = false)
    private StartLevelSource startLevelSource;

    @Column(name = "starting_level", nullable = false)
    private Integer startingLevel;

    @Column(name = "current_level", nullable = false)
    private Integer currentLevel;

    @Column(name = "total_blocks", nullable = false)
    private Integer totalBlocks;

    @Column(name = "question_count_per_block", nullable = false)
    private Integer questionCountPerBlock;

    @Column(name = "generation_instructions", length = 1000)
    private String generationInstructions;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private AssessmentSessionStatus status;

    @Lob
    @Column(name = "blocks_payload", nullable = false)
    private String blocksPayload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AssessmentSession() {
    }

    public AssessmentSession(
        Collaborator collaborator,
        Skill skill,
        String skillNameSnapshot,
        String skillKey,
        StartLevelSource startLevelSource,
        Integer startingLevel,
        Integer currentLevel,
        Integer totalBlocks,
        Integer questionCountPerBlock,
        String generationInstructions,
        AssessmentSessionStatus status,
        String blocksPayload
    ) {
        this.collaborator = collaborator;
        this.skill = skill;
        this.skillNameSnapshot = skillNameSnapshot;
        this.skillKey = skillKey;
        this.startLevelSource = startLevelSource;
        this.startingLevel = startingLevel;
        this.currentLevel = currentLevel;
        this.totalBlocks = totalBlocks;
        this.questionCountPerBlock = questionCountPerBlock;
        this.generationInstructions = generationInstructions;
        this.status = status;
        this.blocksPayload = blocksPayload;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Collaborator getCollaborator() {
        return collaborator;
    }

    public Skill getSkill() {
        return skill;
    }

    public String getSkillNameSnapshot() {
        return skillNameSnapshot;
    }

    public String getSkillKey() {
        return skillKey;
    }

    public StartLevelSource getStartLevelSource() {
        return startLevelSource;
    }

    public Integer getStartingLevel() {
        return startingLevel;
    }

    public Integer getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(Integer currentLevel) {
        this.currentLevel = currentLevel;
    }

    public Integer getTotalBlocks() {
        return totalBlocks;
    }

    public Integer getQuestionCountPerBlock() {
        return questionCountPerBlock;
    }

    public String getGenerationInstructions() {
        return generationInstructions;
    }

    public AssessmentSessionStatus getStatus() {
        return status;
    }

    public void setStatus(AssessmentSessionStatus status) {
        this.status = status;
    }

    public String getBlocksPayload() {
        return blocksPayload;
    }

    public void setBlocksPayload(String blocksPayload) {
        this.blocksPayload = blocksPayload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }
}
