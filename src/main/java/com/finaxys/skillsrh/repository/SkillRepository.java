package com.finaxys.skillsrh.repository;

import com.finaxys.skillsrh.domain.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    List<Skill> findByCategoryId(Long categoryId);
}
