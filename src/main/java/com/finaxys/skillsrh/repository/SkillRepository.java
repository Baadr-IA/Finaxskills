package com.finaxys.skillsrh.repository;

import com.finaxys.skillsrh.domain.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    @Query("select s from Skill s join fetch s.category where s.category.id = :categoryId")
    List<Skill> findByCategoryIdWithCategory(@Param("categoryId") Long categoryId);

    @Query("select s from Skill s join fetch s.category")
    List<Skill> findAllWithCategory();

    @Query("select s from Skill s join fetch s.category where s.id = :id")
    Optional<Skill> findByIdWithCategory(@Param("id") Long id);

    Optional<Skill> findByNameIgnoreCase(String name);
}
