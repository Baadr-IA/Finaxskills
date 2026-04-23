package com.finaxys.skillsrh.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "collaborator_skill")
public class CollaboratorSkill {

    @EmbeddedId
    private CollaboratorSkillId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("collaboratorId")
    @JoinColumn(name = "collaborator_id")
    private Collaborator collaborator;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("skillId")
    @JoinColumn(name = "skill_id")
    private Skill skill;

    /** Level declared by the collaborator (1 = Beginner, 5 = Expert). Null if not self-evaluated. */
    @Column(name = "self_level")
    private Integer selfLevel;

    /** Note left by the collaborator during self-evaluation. */
    @Column(name = "self_note", length = 500)
    private String selfNote;

    /** Level assigned by the HR/manager. Null if not yet evaluated. */
    @Column(name = "hr_level")
    private Integer hrLevel;

    /** Note left by the HR/manager. */
    @Column(name = "hr_note", length = 500)
    private String hrNote;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected CollaboratorSkill() {
    }

    public CollaboratorSkill(Collaborator collaborator, Skill skill) {
        this.id = new CollaboratorSkillId(collaborator.getId(), skill.getId());
        this.collaborator = collaborator;
        this.skill = skill;
        this.updatedAt = Instant.now();
    }

    public CollaboratorSkillId getId() { return id; }

    public Collaborator getCollaborator() { return collaborator; }
    public Skill getSkill() { return skill; }

    public Integer getSelfLevel() { return selfLevel; }
    public void setSelfLevel(Integer selfLevel) { this.selfLevel = selfLevel; }

    public String getSelfNote() { return selfNote; }
    public void setSelfNote(String selfNote) { this.selfNote = selfNote; }

    public Integer getHrLevel() { return hrLevel; }
    public void setHrLevel(Integer hrLevel) { this.hrLevel = hrLevel; }

    public String getHrNote() { return hrNote; }
    public void setHrNote(String hrNote) { this.hrNote = hrNote; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
