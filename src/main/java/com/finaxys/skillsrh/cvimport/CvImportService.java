package com.finaxys.skillsrh.cvimport;

import com.finaxys.skillsrh.api.ApiException;
import com.finaxys.skillsrh.domain.Collaborator;
import com.finaxys.skillsrh.domain.CollaboratorSkill;
import com.finaxys.skillsrh.domain.CollaboratorSkillId;
import com.finaxys.skillsrh.domain.Skill;
import com.finaxys.skillsrh.domain.SkillCategory;
import com.finaxys.skillsrh.repository.CollaboratorRepository;
import com.finaxys.skillsrh.repository.CollaboratorSkillRepository;
import com.finaxys.skillsrh.repository.SkillCategoryRepository;
import com.finaxys.skillsrh.repository.SkillRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class CvImportService {

    private final SmartCvClient smartCvClient;
    private final CollaboratorRepository collaboratorRepository;
    private final SkillRepository skillRepository;
    private final SkillCategoryRepository skillCategoryRepository;
    private final CollaboratorSkillRepository collaboratorSkillRepository;

    public CvImportService(
        SmartCvClient smartCvClient,
        CollaboratorRepository collaboratorRepository,
        SkillRepository skillRepository,
        SkillCategoryRepository skillCategoryRepository,
        CollaboratorSkillRepository collaboratorSkillRepository
    ) {
        this.smartCvClient = smartCvClient;
        this.collaboratorRepository = collaboratorRepository;
        this.skillRepository = skillRepository;
        this.skillCategoryRepository = skillCategoryRepository;
        this.collaboratorSkillRepository = collaboratorSkillRepository;
    }

    @Transactional
    public CvImportDraft analyze(MultipartFile file) {
        try {
            SmartCvAnalysisResponse analysis = smartCvClient.analyze(file);

            CvImportCollaborator collaborator = mapCollaborator(analysis);
            MatchDecision matchDecision = matchCollaborator(collaborator);
            String sourceFilename = firstNonBlank(
                analysis.metadata() != null ? analysis.metadata().sourceFichier() : null,
                file != null ? file.getOriginalFilename() : null,
                "cv-import"
            );

            return new CvImportDraft(
                matchDecision.action(),
                matchDecision.collaboratorId(),
                matchDecision.reason(),
                collaborator,
                mapSkills(analysis, sourceFilename),
                new CvImportSource(
                    sourceFilename,
                    firstNonBlank(analysis.titreProfessionnel(), analysis.typePoste(), collaborator.jobTitle()),
                    blankToNull(analysis.profil())
                )
            );
        } catch (ApiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ApiException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "cv-import-draft-failed",
                "Failed to build CV import draft: " + summarizeException(exception)
            );
        }
    }

    @Transactional
    public CvImportCommitResponse commit(@Valid CvImportCommitRequest request) {
        Collaborator collaborator = saveCollaborator(request);
        int importedSkills = 0;

        List<CvImportSkillDraft> skills = request.skills() != null ? request.skills() : List.of();
        for (CvImportSkillDraft skillDraft : skills) {
            if (!skillDraft.selected()) {
                continue;
            }
            Skill skill = resolveSkill(skillDraft);
            upsertAssessment(collaborator, skill, skillDraft);
            importedSkills++;
        }

        ImportAction action = request.targetCollaboratorId() != null ? ImportAction.UPDATE : ImportAction.CREATE;
        return new CvImportCommitResponse(collaborator.getId(), action, importedSkills);
    }

    private CvImportCollaborator mapCollaborator(SmartCvAnalysisResponse analysis) {
        SmartCvAnalysisResponse.Identity identity = analysis.identite();
        String firstName = blankToDefault(identity != null ? identity.prenom() : null, "Candidat");
        String lastName = blankToDefault(identity != null ? identity.nom() : null, "A renseigner");
        return new CvImportCollaborator(
            firstName,
            lastName,
            blankToEmpty(identity != null ? identity.email() : null),
            blankToEmpty(firstNonBlank(analysis.titreProfessionnel(), analysis.typePoste(), null)),
            ""
        );
    }

    private MatchDecision matchCollaborator(CvImportCollaborator collaborator) {
        String email = blankToNull(collaborator.email());
        if (email != null) {
            return collaboratorRepository.findByEmailIgnoreCase(email)
                .map(existing -> new MatchDecision(
                    ImportAction.UPDATE,
                    existing.getId(),
                    "Collaborateur existant detecte via l'email"
                ))
                .orElseGet(() -> new MatchDecision(ImportAction.CREATE, null, "Aucun collaborateur existant avec cet email"));
        }

        List<Collaborator> candidates = collaboratorRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCase(
            collaborator.firstName(),
            collaborator.lastName()
        );
        if (candidates.size() == 1) {
            return new MatchDecision(
                ImportAction.UPDATE,
                candidates.getFirst().getId(),
                "Collaborateur existant detecte via le nom complet"
            );
        }
        if (candidates.size() > 1) {
            return new MatchDecision(
                ImportAction.CREATE,
                null,
                "Plusieurs collaborateurs correspondent au nom complet - validation RH requise"
            );
        }
        return new MatchDecision(ImportAction.CREATE, null, "Nouveau collaborateur a creer");
    }

    private List<CvImportSkillDraft> mapSkills(SmartCvAnalysisResponse analysis, String sourceFilename) {
        Map<String, CvImportSkillDraft> drafts = new LinkedHashMap<>();
        List<SmartCvAnalysisResponse.SkillItem> sourceSkills = analysis.competences() != null ? analysis.competences() : List.of();

        for (SmartCvAnalysisResponse.SkillItem sourceSkill : sourceSkills) {
            String skillName = blankToNull(sourceSkill.nom());
            if (skillName == null) {
                continue;
            }

            Optional<Skill> existingSkill = skillRepository.findWithCategoryByNameIgnoreCase(skillName);
            String categoryName = existingSkill
                .map(skill -> skill.getCategory().getName())
                .orElseGet(() -> firstNonBlank(sourceSkill.categorie(), "Import CV"));
            Optional<SkillCategory> existingCategory = existingSkill
                .map(Skill::getCategory)
                .or(() -> skillCategoryRepository.findByNameIgnoreCase(categoryName));

            CvImportSkillDraft draft = new CvImportSkillDraft(
                existingSkill.map(Skill::getId).orElse(null),
                skillName,
                existingCategory.map(SkillCategory::getId).orElse(null),
                categoryName,
                mapLevel(sourceSkill.niveau(), sourceSkill.anneesExperience()),
                buildHrNote(sourceSkill, sourceFilename),
                blankToNull(sourceSkill.niveau()),
                sourceSkill.anneesExperience(),
                existingSkill.isPresent() ? SkillResolution.REUSE : SkillResolution.CREATE,
                true
            );
            drafts.putIfAbsent(canonicalKey(skillName), draft);
        }

        return new ArrayList<>(drafts.values());
    }

    private Collaborator saveCollaborator(CvImportCommitRequest request) {
        Collaborator collaborator = request.targetCollaboratorId() != null
            ? requireCollaborator(request.targetCollaboratorId())
            : new Collaborator("", "", "", null, null);

        collaborator.setFirstName(request.collaborator().firstName().trim());
        collaborator.setLastName(request.collaborator().lastName().trim());
        collaborator.setEmail(request.collaborator().email().trim());
        collaborator.setJobTitle(blankToNull(request.collaborator().jobTitle()));
        collaborator.setKeycloakId(blankToNull(request.collaborator().keycloakId()));

        try {
            return collaboratorRepository.save(collaborator);
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(
                HttpStatus.CONFLICT,
                "cv-import-collaborator-conflict",
                "A collaborator with the same email or identifier already exists"
            );
        }
    }

    private Skill resolveSkill(CvImportSkillDraft draft) {
        if (draft.skillId() != null) {
            return requireSkill(draft.skillId());
        }

        String skillName = blankToNull(draft.skillName());
        if (skillName == null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "cv-import-skill-name-required", "Each selected skill requires a name");
        }

        Optional<Skill> existingSkill = skillRepository.findWithCategoryByNameIgnoreCase(skillName);
        if (existingSkill.isPresent()) {
            return existingSkill.get();
        }

        SkillCategory category = resolveCategory(draft);
        try {
            return skillRepository.save(new Skill(skillName, buildSkillDescription(draft), category));
        } catch (DataIntegrityViolationException exception) {
            return skillRepository.findByNameIgnoreCase(skillName)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "cv-import-skill-conflict", "Unable to create or reuse the imported skill"));
        }
    }

    private SkillCategory resolveCategory(CvImportSkillDraft draft) {
        if (draft.categoryId() != null) {
            return requireCategory(draft.categoryId());
        }

        String categoryName = blankToDefault(draft.categoryName(), "Import CV");
        return skillCategoryRepository.findByNameIgnoreCase(categoryName)
            .orElseGet(() -> {
                try {
                    return skillCategoryRepository.save(new SkillCategory(categoryName, "Categorie creee lors d'un import Smart CV"));
                } catch (DataIntegrityViolationException exception) {
                    return skillCategoryRepository.findByNameIgnoreCase(categoryName)
                        .orElseThrow(() -> new ApiException(
                            HttpStatus.CONFLICT,
                            "cv-import-category-conflict",
                            "Unable to create or reuse the imported skill category"
                        ));
                }
            });
    }

    private void upsertAssessment(Collaborator collaborator, Skill skill, CvImportSkillDraft draft) {
        CollaboratorSkillId id = new CollaboratorSkillId(collaborator.getId(), skill.getId());
        CollaboratorSkill assessment = collaboratorSkillRepository.findById(id)
            .orElseGet(() -> new CollaboratorSkill(collaborator, skill));
        assessment.setHrLevel(draft.hrLevel());
        assessment.setHrNote(blankToNull(draft.hrNote()));
        assessment.setUpdatedAt(Instant.now());
        collaboratorSkillRepository.save(assessment);
    }

    private Collaborator requireCollaborator(Long id) {
        return collaboratorRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "collaborator-not-found", "Collaborator not found"));
    }

    private Skill requireSkill(Long id) {
        return skillRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "skill-not-found", "Skill not found"));
    }

    private SkillCategory requireCategory(Long id) {
        return skillCategoryRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "skill-category-not-found", "Skill category not found"));
    }

    private String buildSkillDescription(CvImportSkillDraft draft) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(draft.sourceLevel())) {
            parts.add("Niveau Smart CV: " + draft.sourceLevel().trim());
        }
        if (draft.yearsExperience() != null) {
            parts.add("Experience estimee: " + draft.yearsExperience() + " an(s)");
        }
        return truncate(String.join(" - ", parts), 255);
    }

    private String buildHrNote(SmartCvAnalysisResponse.SkillItem sourceSkill, String sourceFilename) {
        List<String> parts = new ArrayList<>();
        parts.add("Import CV");
        if (StringUtils.hasText(sourceFilename)) {
            parts.add("source=" + sourceFilename.trim());
        }
        if (StringUtils.hasText(sourceSkill.niveau())) {
            parts.add("niveau=" + sourceSkill.niveau().trim());
        }
        if (sourceSkill.anneesExperience() != null) {
            parts.add("experience=" + sourceSkill.anneesExperience() + " an(s)");
        }
        if (StringUtils.hasText(sourceSkill.justification())) {
            parts.add("justification=" + sourceSkill.justification().trim());
        }
        return truncate(String.join(" | ", parts), 500);
    }

    private Integer mapLevel(String level, Integer yearsExperience) {
        String normalizedLevel = canonicalKey(level);
        if (normalizedLevel.contains("expert") || normalizedLevel.contains("senior")) {
            return 5;
        }
        if (normalizedLevel.contains("avance") || normalizedLevel.contains("confirme")) {
            return 4;
        }
        if (normalizedLevel.contains("intermediaire") || normalizedLevel.contains("intermediate")) {
            return 3;
        }
        if (normalizedLevel.contains("notion")) {
            return 2;
        }
        if (normalizedLevel.contains("debutant") || normalizedLevel.contains("junior") || normalizedLevel.contains("beginner")) {
            return 1;
        }
        if (yearsExperience != null) {
            if (yearsExperience >= 8) {
                return 5;
            }
            if (yearsExperience >= 5) {
                return 4;
            }
            if (yearsExperience >= 2) {
                return 3;
            }
            if (yearsExperience >= 1) {
                return 2;
            }
            return 1;
        }
        return null;
    }

    private String canonicalKey(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .trim();
        return normalized.replaceAll("\\s+", " ");
    }

    private String blankToDefault(String value, String defaultValue) {
        String normalized = blankToNull(value);
        return normalized != null ? normalized : defaultValue;
    }

    private String blankToEmpty(String value) {
        String normalized = blankToNull(value);
        return normalized != null ? normalized : "";
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value) || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }

    private String summarizeException(RuntimeException exception) {
        Throwable current = exception;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = StringUtils.hasText(current.getMessage()) ? current.getMessage().trim() : current.getClass().getSimpleName();
        return current.getClass().getSimpleName() + " - " + truncate(message, 220);
    }

    private record MatchDecision(ImportAction action, Long collaboratorId, String reason) {
    }

    public enum ImportAction {
        CREATE,
        UPDATE
    }

    public enum SkillResolution {
        REUSE,
        CREATE
    }

    public record CvImportDraft(
        ImportAction action,
        Long matchedCollaboratorId,
        String matchReason,
        CvImportCollaborator collaborator,
        List<CvImportSkillDraft> skills,
        CvImportSource source
    ) {
    }

    public record CvImportSource(
        String sourceFilename,
        String professionalTitle,
        String profileSummary
    ) {
    }

    public record CvImportCommitResponse(
        Long collaboratorId,
        ImportAction action,
        int importedSkills
    ) {
    }

    public record CvImportCommitRequest(
        Long targetCollaboratorId,
        @Valid CvImportCollaborator collaborator,
        List<@Valid CvImportSkillDraft> skills
    ) {
    }

    public record CvImportCollaborator(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 150) String jobTitle,
        @Size(max = 255) String keycloakId
    ) {
    }

    public record CvImportSkillDraft(
        Long skillId,
        @NotBlank @Size(max = 100) String skillName,
        Long categoryId,
        @NotBlank @Size(max = 100) String categoryName,
        @Min(1) @Max(5) Integer hrLevel,
        @Size(max = 500) String hrNote,
        String sourceLevel,
        Integer yearsExperience,
        SkillResolution resolution,
        boolean selected
    ) {
    }
}
