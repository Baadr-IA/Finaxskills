package com.finaxys.skillsrh.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Table;

@Entity
@Table(name = "skill")
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", length = 20, nullable = false)
    private Level level = Level.NIVEAU_0;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private SkillCategory category;

    protected Skill() {
    }

    public Skill(String name, String description, SkillCategory category, Level level) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.level = level == null ? Level.NIVEAU_1 : level;
    }

    // Backward-compatible constructor used in data initialization and tests
    public Skill(String name, String description, SkillCategory category) {
        this(name, description, category, Level.NIVEAU_1);
    }

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Level getLevel() { return level; }
    public void setLevel(Level level) { this.level = level; }

    public SkillCategory getCategory() { return category; }
    public void setCategory(SkillCategory category) { this.category = category; }
}
