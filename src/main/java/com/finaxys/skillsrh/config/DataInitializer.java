package com.finaxys.skillsrh.config;

import com.finaxys.skillsrh.domain.Collaborator;
import com.finaxys.skillsrh.domain.CollaboratorSkill;
import com.finaxys.skillsrh.domain.Skill;
import com.finaxys.skillsrh.domain.SkillCategory;
import com.finaxys.skillsrh.repository.CollaboratorRepository;
import com.finaxys.skillsrh.repository.CollaboratorSkillRepository;
import com.finaxys.skillsrh.repository.SkillCategoryRepository;
import com.finaxys.skillsrh.repository.SkillRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class DataInitializer implements ApplicationRunner {

    private final SkillCategoryRepository categoryRepository;
    private final SkillRepository skillRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final CollaboratorSkillRepository assessmentRepository;

    public DataInitializer(
        SkillCategoryRepository categoryRepository,
        SkillRepository skillRepository,
        CollaboratorRepository collaboratorRepository,
        CollaboratorSkillRepository assessmentRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.skillRepository = skillRepository;
        this.collaboratorRepository = collaboratorRepository;
        this.assessmentRepository = assessmentRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (categoryRepository.count() > 0) {
            return; // Already initialized
        }

        // ── Skill Categories ─────────────────────────────────────────────────
        SkillCategory java = categoryRepository.save(new SkillCategory("Java & JVM", "Backend Java ecosystem"));
        SkillCategory frontend = categoryRepository.save(new SkillCategory("Frontend", "Web front-end technologies"));
        SkillCategory devops = categoryRepository.save(new SkillCategory("DevOps & Cloud", "Infrastructure and deployment"));
        SkillCategory softSkills = categoryRepository.save(new SkillCategory("Soft Skills", "Transversal human skills"));

        // ── Skills ───────────────────────────────────────────────────────────
        Skill springBoot = skillRepository.save(new Skill("Spring Boot", "Spring Boot framework", java));
        Skill hibernate = skillRepository.save(new Skill("Hibernate / JPA", "ORM for relational databases", java));
        Skill angular = skillRepository.save(new Skill("Angular", "Angular 21 SPA framework", frontend));
        Skill react = skillRepository.save(new Skill("React", "React 18 library", frontend));
        Skill typescript = skillRepository.save(new Skill("TypeScript", "Typed JavaScript superset", frontend));
        Skill docker = skillRepository.save(new Skill("Docker", "Container platform", devops));
        Skill kubernetes = skillRepository.save(new Skill("Kubernetes", "Container orchestration", devops));
        Skill communication = skillRepository.save(new Skill("Communication", "Oral and written communication", softSkills));
        Skill leadership = skillRepository.save(new Skill("Leadership", "Team leadership and motivation", softSkills));

        // ── Collaborators ─────────────────────────────────────────────────────
        Collaborator alice = collaboratorRepository.save(
            new Collaborator("Alice", "Martin", "alice.martin@finaxys.com", "Senior Backend Developer", "kc-alice-001")
        );
        Collaborator bob = collaboratorRepository.save(
            new Collaborator("Bob", "Dupont", "bob.dupont@finaxys.com", "Full-Stack Developer", "kc-bob-002")
        );
        Collaborator claire = collaboratorRepository.save(
            new Collaborator("Claire", "Leroy", "claire.leroy@finaxys.com", "DevOps Engineer", "kc-claire-003")
        );

        // ── Assessments (self + HR) ───────────────────────────────────────────
        addAssessment(alice, springBoot, 5, "My core skill", 5, "Expert level confirmed");
        addAssessment(alice, hibernate, 4, "Comfortable with complex mappings", 4, null);
        addAssessment(alice, angular, 2, "Learning in progress", 2, "Beginner — encourage training");
        addAssessment(alice, communication, 4, null, 5, "Excellent communicator");

        addAssessment(bob, angular, 5, "Main framework", 4, "Strong skills");
        addAssessment(bob, typescript, 4, "Daily use", 4, null);
        addAssessment(bob, springBoot, 3, "I can write REST APIs", 3, null);
        addAssessment(bob, react, 3, "Side projects", null, null);

        addAssessment(claire, docker, 5, "Expert", 5, "Container specialist");
        addAssessment(claire, kubernetes, 4, "Production experience", 4, "K8s certified");
        addAssessment(claire, leadership, 3, null, 4, "Good team lead potential");
    }

    private void addAssessment(
        Collaborator collaborator,
        Skill skill,
        Integer selfLevel, String selfNote,
        Integer hrLevel, String hrNote
    ) {
        CollaboratorSkill cs = new CollaboratorSkill(collaborator, skill);
        cs.setSelfLevel(selfLevel);
        cs.setSelfNote(selfNote);
        cs.setHrLevel(hrLevel);
        cs.setHrNote(hrNote);
        cs.setUpdatedAt(Instant.now());
        assessmentRepository.save(cs);
    }
}


