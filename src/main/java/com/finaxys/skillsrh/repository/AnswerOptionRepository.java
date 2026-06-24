package com.finaxys.skillsrh.repository;

import com.finaxys.skillsrh.domain.AnswerOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface AnswerOptionRepository extends JpaRepository<AnswerOption, Long> {

    void deleteByQuestion_IdIn(Collection<Long> questionIds);

    Optional<AnswerOption> findByQuestion_IdAndOptionCodeIgnoreCase(Long questionId, String optionCode);
}
