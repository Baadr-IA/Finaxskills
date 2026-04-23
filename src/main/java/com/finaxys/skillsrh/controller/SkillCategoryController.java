package com.finaxys.skillsrh.controller;

import com.finaxys.skillsrh.api.ApiException;
import com.finaxys.skillsrh.domain.SkillCategory;
import com.finaxys.skillsrh.repository.SkillCategoryRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SkillCategoryController {

    private final SkillCategoryRepository categoryRepository;

    public SkillCategoryController(SkillCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/skill-categories")
    @PreAuthorize("@permissions.has(authentication, 'SKILL_CATEGORIES', 'READ', 'ALL')")
    public List<SkillCategoryResponse> list() {
        return categoryRepository.findAll().stream()
            .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
            .map(this::toResponse)
            .toList();
    }

    @GetMapping("/skill-categories/{id}")
    @PreAuthorize("@permissions.has(authentication, 'SKILL_CATEGORIES', 'READ', 'ALL')")
    public SkillCategoryResponse getById(@PathVariable Long id) {
        return toResponse(require(id));
    }

    @PostMapping("/skill-categories")
    @PreAuthorize("@permissions.has(authentication, 'SKILL_CATEGORIES', 'CREATE', 'ALL')")
    public ResponseEntity<SkillCategoryResponse> create(@Valid @RequestBody CategoryRequest req) {
        SkillCategory saved = categoryRepository.save(new SkillCategory(req.name(), req.description()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @PutMapping("/skill-categories/{id}")
    @PreAuthorize("@permissions.has(authentication, 'SKILL_CATEGORIES', 'UPDATE', 'ALL')")
    public SkillCategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest req) {
        SkillCategory cat = require(id);
        cat.setName(req.name());
        cat.setDescription(req.description());
        return toResponse(categoryRepository.save(cat));
    }

    @DeleteMapping("/skill-categories/{id}")
    @PreAuthorize("@permissions.has(authentication, 'SKILL_CATEGORIES', 'DELETE', 'ALL')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryRepository.findById(id).ifPresent(categoryRepository::delete);
        return ResponseEntity.noContent().build();
    }

    private SkillCategory require(Long id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "skill-category-not-found", "Skill category not found"));
    }

    private SkillCategoryResponse toResponse(SkillCategory c) {
        return new SkillCategoryResponse(c.getId(), c.getName(), c.getDescription());
    }

    public record SkillCategoryResponse(Long id, String name, String description) {}

    public record CategoryRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 255) String description
    ) {}
}
