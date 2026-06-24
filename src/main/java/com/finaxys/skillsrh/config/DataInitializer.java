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
import java.util.Random;
import java.util.List;

@Component
public class DataInitializer implements ApplicationRunner {

    private final SkillCategoryRepository categoryRepository;
    private final SkillRepository skillRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final CollaboratorSkillRepository assessmentRepository;
    private final Random random = new Random();

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

        // ── Collaborators ─────────────────────────────────────────────────────
        if (collaboratorRepository.count() == 0) {
            Collaborator alice = collaboratorRepository.save(
                new Collaborator("Alice", "Martin", "alice.martin@finaxys.com", "Senior Backend Developer", "kc-alice-001")
            );
            Collaborator bob = collaboratorRepository.save(
                new Collaborator("Bob", "Dupont", "bob.dupont@finaxys.com", "Full-Stack Developer", "kc-bob-002")
            );
            Collaborator claire = collaboratorRepository.save(
                new Collaborator("Claire", "Leroy", "claire.leroy@finaxys.com", "DevOps Engineer", "kc-claire-003")
            );
            Collaborator rabhi = collaboratorRepository.save(
                new Collaborator("Malak", "RABHI", "malak.rabhi@finaxys.com", "Développeur fullstack", "kc-malak-004")
            );
            Collaborator hochlef = collaboratorRepository.save(
                new Collaborator("Marwa", "HOCHLEF", "marwa.hochlef@finaxys.com", "Développeur fullstack", "kc-marwa-005")
            );
            Collaborator chouchene = collaboratorRepository.save(
                new Collaborator("Mohamed", "Chouchene", "mohamed.chouchene@finaxys.com", "Développeur Java", "kc-mohamed-006")
            );
            Collaborator betajeb = collaboratorRepository.save(
                new Collaborator("Hamza", "BETAJEB", "hamza.betajeb@finaxys.com", "Développeur Java", "kc-hamza-007")
            );
            Collaborator abdelkader = collaboratorRepository.save(
                new Collaborator("Ahmed", "ABDELKADER", "ahmed.abdelkader@finaxys.com", "Développeur fullstack", "kc-ahmed-008")
            );

            addAssessment(alice, springBoot, 5, "My core skill", 5, "Expert level confirmed");
            addAssessment(alice, javaLanguage, 4, "Strong Java backend experience", 4, "Reliable on Java projects");
            addAssessment(alice, pythonLanguage, 2, "Basic scripting only", 2, "Needs practice on Python");
            addAssessment(alice, hibernate, 4, "Comfortable with complex mappings", 4, null);
            addAssessment(alice, angular, 2, "Learning in progress", 2, "Beginner — encourage training");
            addAssessment(alice, communication, 4, null, 5, "Excellent communicator");

            addAssessment(bob, angular, 5, "Main framework", 4, "Strong skills");
            addAssessment(bob, typescript, 4, "Daily use", 4, null);
            addAssessment(bob, javaLanguage, 3, "Comfortable with Java APIs", 3, null);
            addAssessment(bob, pythonLanguage, 2, "Occasional automation", 2, null);
            addAssessment(bob, springBoot, 3, "I can write REST APIs", 3, null);
            addAssessment(bob, react, 3, "Side projects", null, null);

            addAssessment(claire, docker, 5, "Expert", 5, "Container specialist");
            addAssessment(claire, kubernetes, 4, "Production experience", 4, "K8s certified");
            addAssessment(claire, javaLanguage, 2, "Can read Java services", 2, null);
            addAssessment(claire, pythonLanguage, 4, "Uses Python for tooling and automation", 4, "Very autonomous on Python automation");
            addAssessment(claire, leadership, 3, null, 4, "Good team lead potential");

            addRandomAssessment(rabhi, angular);
            addRandomAssessment(rabhi, react);
            addRandomAssessment(rabhi, typescript);
            addRandomAssessment(rabhi, javaLanguage);
            addRandomAssessment(rabhi, springBoot);
            addRandomAssessment(rabhi, communication);

            addRandomAssessment(hochlef, angular);
            addRandomAssessment(hochlef, react);
            addRandomAssessment(hochlef, typescript);
            addRandomAssessment(hochlef, springBoot);
            addRandomAssessment(hochlef, pythonLanguage);

            addRandomAssessment(chouchene, javaLanguage);
            addRandomAssessment(chouchene, springBoot);
            addRandomAssessment(chouchene, hibernate);
            addRandomAssessment(chouchene, pythonLanguage);
            addRandomAssessment(chouchene, docker);

            addRandomAssessment(betajeb, javaLanguage);
            addRandomAssessment(betajeb, springBoot);
            addRandomAssessment(betajeb, hibernate);
            addRandomAssessment(betajeb, docker);
            addRandomAssessment(betajeb, leadership);

            addRandomAssessment(abdelkader, angular);
            addRandomAssessment(abdelkader, typescript);
            addRandomAssessment(abdelkader, javaLanguage);
            addRandomAssessment(abdelkader, springBoot);
            addRandomAssessment(abdelkader, react);
            addRandomAssessment(abdelkader, communication);
        }
    }

    private SkillCategory ensureCategory(String name, String description) {
        return categoryRepository.findByNameIgnoreCase(name)
            .orElseGet(() -> categoryRepository.save(new SkillCategory(name, description)));
    }

    private Skill ensureSkill(String name, String description, SkillCategory category) {
        return skillRepository.findByNameIgnoreCase(name)
            .orElseGet(() -> skillRepository.save(new Skill(name, description, category)));
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

    private void addRandomAssessment(Collaborator collaborator, Skill skill) {
        int selfLevel = 1 + random.nextInt(5); // Random level 1-5
        int hrLevel = 1 + random.nextInt(5);   // Random level 1-5
        String selfNote = null;
        String hrNote = null;

        CollaboratorSkill cs = new CollaboratorSkill(collaborator, skill);
        cs.setSelfLevel(selfLevel);
        cs.setHrLevel(hrLevel);
        cs.setUpdatedAt(Instant.now());
        assessmentRepository.save(cs);
    }
}

