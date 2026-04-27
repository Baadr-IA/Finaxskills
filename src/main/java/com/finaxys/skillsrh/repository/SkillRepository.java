package com.finaxys.skillsrh.repository;

import com.finaxys.skillsrh.domain.Skill;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    List<Skill> findByCategoryId(Long categoryId);

    Optional<Skill> findByNameIgnoreCase(String name);

    @EntityGraph(attributePaths = "category")
    Optional<Skill> findWithCategoryByNameIgnoreCase(String name);
}
