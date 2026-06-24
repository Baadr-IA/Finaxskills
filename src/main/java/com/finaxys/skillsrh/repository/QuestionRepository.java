package com.finaxys.skillsrh.repository;

import com.finaxys.skillsrh.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByTestCollab_IdOrderByPositionOrderAsc(Long testCollabId);

    void deleteByTestCollab_Id(Long testCollabId);
}

