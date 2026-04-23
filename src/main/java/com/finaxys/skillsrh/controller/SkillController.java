package com.finaxys.skillsrh.controller;

import com.finaxys.skillsrh.api.ApiException;
import com.finaxys.skillsrh.domain.Skill;
import com.finaxys.skillsrh.domain.SkillCategory;
import com.finaxys.skillsrh.repository.SkillCategoryRepository;
import com.finaxys.skillsrh.repository.SkillRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SkillController {

    private final SkillRepository skillRepository;
    private final SkillCategoryRepository categoryRepository;

    public SkillController(SkillRepository skillRepository, SkillCategoryRepository categoryRepository) {
        this.skillRepository = skillRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/skills")
    @PreAuthorize("@permissions.has(authentication, 'SKILLS', 'READ', 'ALL')")
    public List<SkillResponse> list(@RequestParam(required = false) Long categoryId) {
        List<Skill> skills = categoryId != null
            ? skillRepository.findByCategoryId(categoryId)
            : skillRepository.findAll();
        return skills.stream()
            .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
            .map(this::toResponse)
            .toList();
    }

    @GetMapping("/skills/{id}")
    @PreAuthorize("@permissions.has(authentication, 'SKILLS', 'READ', 'ALL')")
    public SkillResponse getById(@PathVariable Long id) {
        return toResponse(require(id));
    }

    @PostMapping("/skills")
    @PreAuthorize("@permissions.has(authentication, 'SKILLS', 'CREATE', 'ALL')")
    public ResponseEntity<SkillResponse> create(@Valid @RequestBody SkillRequest req) {
        SkillCategory category = requireCategory(req.categoryId());
        Skill saved = skillRepository.save(new Skill(req.name(), req.description(), category));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @PutMapping("/skills/{id}")
    @PreAuthorize("@permissions.has(authentication, 'SKILLS', 'UPDATE', 'ALL')")
    public SkillResponse update(@PathVariable Long id, @Valid @RequestBody SkillRequest req) {
        Skill skill = require(id);
        skill.setName(req.name());
        skill.setDescription(req.description());
        skill.setCategory(requireCategory(req.categoryId()));
        return toResponse(skillRepository.save(skill));
    }

    @DeleteMapping("/skills/{id}")
    @PreAuthorize("@permissions.has(authentication, 'SKILLS', 'DELETE', 'ALL')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        skillRepository.findById(id).ifPresent(skillRepository::delete);
        return ResponseEntity.noContent().build();
    }

    private Skill require(Long id) {
        return skillRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "skill-not-found", "Skill not found"));
    }

    private SkillCategory requireCategory(Long id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "skill-category-not-found", "Skill category not found"));
    }

    private SkillResponse toResponse(Skill s) {
        return new SkillResponse(
            s.getId(),
            s.getName(),
            s.getDescription(),
            s.getCategory().getId(),
            s.getCategory().getName()
        );
    }

    public record SkillResponse(Long id, String name, String description, Long categoryId, String categoryName) {}

    public record SkillRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 255) String description,
        @NotNull Long categoryId
    ) {}
}
