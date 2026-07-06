package com.finaxys.skillsrh.config;

import com.finaxys.skillsrh.domain.Skill;
import com.finaxys.skillsrh.domain.SkillCategory;
import com.finaxys.skillsrh.repository.SkillCategoryRepository;
import com.finaxys.skillsrh.repository.SkillRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements ApplicationRunner {

    private final SkillCategoryRepository categoryRepository;
    private final SkillRepository skillRepository;

    public DataInitializer(
        SkillCategoryRepository categoryRepository,
        SkillRepository skillRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.skillRepository = skillRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        // ── Skill Categories ─────────────────────────────────────────────────
        SkillCategory java = ensureCategory("Java & JVM", "Backend Java ecosystem");
        SkillCategory frontend = ensureCategory("Frontend", "Web front-end technologies");
        SkillCategory devops = ensureCategory("DevOps & Cloud", "Infrastructure and deployment");
        SkillCategory softSkills = ensureCategory("Soft Skills", "Transversal human skills");

        // ── Skills ───────────────────────────────────────────────────────────
        Skill javaLanguage = ensureSkill("Java", "Java programming language", java);
        Skill pythonLanguage = ensureSkill("Python", "Python programming language", java);
        Skill springBoot = ensureSkill("Spring Boot", "Spring Boot framework", java);
        Skill hibernate = ensureSkill("Hibernate / JPA", "ORM for relational databases", java);
        Skill angular = ensureSkill("Angular", "Angular 21 SPA framework", frontend);
        Skill react = ensureSkill("React", "React 18 library", frontend);
        Skill typescript = ensureSkill("TypeScript", "Typed JavaScript superset", frontend);
        Skill docker = ensureSkill("Docker", "Container platform", devops);
        Skill kubernetes = ensureSkill("Kubernetes", "Container orchestration", devops);
        Skill communication = ensureSkill("Communication", "Oral and written communication", softSkills);
        Skill leadership = ensureSkill("Leadership", "Team leadership and motivation", softSkills);
    }

    private SkillCategory ensureCategory(String name, String description) {
        return categoryRepository.findByNameIgnoreCase(name)
            .orElseGet(() -> categoryRepository.save(new SkillCategory(name, description)));
    }

    private Skill ensureSkill(String name, String description, SkillCategory category) {
        return skillRepository.findByNameIgnoreCase(name)
            .orElseGet(() -> skillRepository.save(new Skill(name, description, category)));
    }
}

