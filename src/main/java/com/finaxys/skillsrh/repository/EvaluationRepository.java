package com.finaxys.skillsrh.repository;

import com.finaxys.skillsrh.domain.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    List<Evaluation> findAllByOrderByDateAssignedDesc();
}

