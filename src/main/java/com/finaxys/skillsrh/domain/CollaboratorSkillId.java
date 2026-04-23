package com.finaxys.skillsrh.domain;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CollaboratorSkillId implements Serializable {

    private Long collaboratorId;
    private Long skillId;

    public CollaboratorSkillId() {
    }

    public CollaboratorSkillId(Long collaboratorId, Long skillId) {
        this.collaboratorId = collaboratorId;
        this.skillId = skillId;
    }

    public Long getCollaboratorId() { return collaboratorId; }
    public void setCollaboratorId(Long collaboratorId) { this.collaboratorId = collaboratorId; }

    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CollaboratorSkillId that)) return false;
        return Objects.equals(collaboratorId, that.collaboratorId) &&
               Objects.equals(skillId, that.skillId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(collaboratorId, skillId);
    }
}
