package com.finaxys.skillsrh.repository;

import com.finaxys.skillsrh.domain.Greeting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GreetingRepository extends JpaRepository<Greeting, Long> {

    Optional<Greeting> findByKey(String key);
}

