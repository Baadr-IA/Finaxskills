package com.finaxys.skillsrh.cvimport;

import com.finaxys.skillsrh.domain.Collaborator;
import com.finaxys.skillsrh.domain.CollaboratorSkill;
import com.finaxys.skillsrh.domain.CollaboratorSkillId;
import com.finaxys.skillsrh.domain.Skill;
import com.finaxys.skillsrh.domain.SkillCategory;
import com.finaxys.skillsrh.repository.CollaboratorRepository;
import com.finaxys.skillsrh.repository.CollaboratorSkillRepository;
import com.finaxys.skillsrh.repository.SkillCategoryRepository;
import com.finaxys.skillsrh.repository.SkillRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CvImportServiceTest {

    private final SmartCvClient smartCvClient = mock(SmartCvClient.class);
    private final CollaboratorRepository collaboratorRepository = mock(CollaboratorRepository.class);
    private final SkillRepository skillRepository = mock(SkillRepository.class);
    private final SkillCategoryRepository skillCategoryRepository = mock(SkillCategoryRepository.class);
    private final CollaboratorSkillRepository collaboratorSkillRepository = mock(CollaboratorSkillRepository.class);

    private final CvImportService service = new CvImportService(
        smartCvClient,
        collaboratorRepository,
        skillRepository,
        skillCategoryRepository,
        collaboratorSkillRepository
    );

    @Test
    void analyzeBuildsDraftAndMatchesExistingCollaboratorByEmail() {
        MockMultipartFile file = new MockMultipartFile("file", "alice-cv.pdf", "application/pdf", "pdf".getBytes());
        Collaborator existingCollaborator = collaborator(7L, "Alice", "Martin", "alice@finaxys.com", "Consultante", null);
        SkillCategory existingCategory = category(3L, "Frontend");
        Skill existingSkill = skill(11L, "Angular", existingCategory);

        when(smartCvClient.analyze(file)).thenReturn(new SmartCvAnalysisResponse(
            new SmartCvAnalysisResponse.Identity("Martin", "Alice", "alice@finaxys.com"),
            "Consultante Angular",
            null,
            "Profil Angular senior",
            List.of(new SmartCvAnalysisResponse.SkillItem("Angular", "Frontend", "Confirme", 5, "Mission longue")),
            new SmartCvAnalysisResponse.Metadata("alice-cv.pdf")
        ));
        when(collaboratorRepository.findByEmailIgnoreCase("alice@finaxys.com")).thenReturn(Optional.of(existingCollaborator));
        when(skillRepository.findWithCategoryByNameIgnoreCase("Angular")).thenReturn(Optional.of(existingSkill));

        CvImportService.CvImportDraft draft = service.analyze(file);

        assertEquals(CvImportService.ImportAction.UPDATE, draft.action());
        assertEquals(7L, draft.matchedCollaboratorId());
        assertEquals("Alice", draft.collaborator().firstName());
        assertEquals("Martin", draft.collaborator().lastName());
        assertEquals("Consultante Angular", draft.collaborator().jobTitle());
        assertEquals(1, draft.skills().size());
        assertEquals(CvImportService.SkillResolution.REUSE, draft.skills().getFirst().resolution());
        assertEquals(4, draft.skills().getFirst().hrLevel());
        assertEquals("Frontend", draft.skills().getFirst().categoryName());
    }

    @Test
    void commitCreatesMissingSkillAndAssessmentForNewCollaborator() {
        Collaborator savedCollaborator = collaborator(15L, "Nora", "Bensaid", "nora@finaxys.com", "Data Engineer", null);
        SkillCategory savedCategory = category(8L, "Data");
        Skill savedSkill = skill(21L, "Python", savedCategory);

        when(collaboratorRepository.save(any(Collaborator.class))).thenReturn(savedCollaborator);
        when(skillCategoryRepository.findByNameIgnoreCase("Data")).thenReturn(Optional.empty());
        when(skillCategoryRepository.save(any(SkillCategory.class))).thenReturn(savedCategory);
        when(skillRepository.findWithCategoryByNameIgnoreCase("Python")).thenReturn(Optional.empty());
        when(skillRepository.save(any(Skill.class))).thenReturn(savedSkill);
        when(collaboratorSkillRepository.findById(new CollaboratorSkillId(15L, 21L))).thenReturn(Optional.empty());

        CvImportService.CvImportCommitResponse response = service.commit(new CvImportService.CvImportCommitRequest(
            null,
            new CvImportService.CvImportCollaborator("Nora", "Bensaid", "nora@finaxys.com", "Data Engineer", null),
            List.of(new CvImportService.CvImportSkillDraft(
                null,
                "Python",
                null,
                "Data",
                4,
                "Import CV | niveau=Confirme",
                "Confirme",
                4,
                CvImportService.SkillResolution.CREATE,
                true
            ))
        ));

        assertEquals(CvImportService.ImportAction.CREATE, response.action());
        assertEquals(15L, response.collaboratorId());
        assertEquals(1, response.importedSkills());
        verify(collaboratorSkillRepository).save(any(CollaboratorSkill.class));
    }

    private Collaborator collaborator(Long id, String firstName, String lastName, String email, String jobTitle, String keycloakId) {
        Collaborator collaborator = new Collaborator(firstName, lastName, email, jobTitle, keycloakId);
        ReflectionTestUtils.setField(collaborator, "id", id);
        return collaborator;
    }

    private SkillCategory category(Long id, String name) {
        SkillCategory category = new SkillCategory(name, null);
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }

    private Skill skill(Long id, String name, SkillCategory category) {
        Skill skill = new Skill(name, null, category);
        ReflectionTestUtils.setField(skill, "id", id);
        return skill;
    }
}
