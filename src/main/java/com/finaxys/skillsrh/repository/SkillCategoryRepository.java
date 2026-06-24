package com.finaxys.skillsrh.repository;

import com.finaxys.skillsrh.domain.SkillCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SkillCategoryRepository extends JpaRepository<SkillCategory, Long> {

    Optional<SkillCategory> findByNameIgnoreCase(String name);
}
