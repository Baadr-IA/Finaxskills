package com.finaxys.templateappname.repository;

import com.finaxys.templateappname.domain.Greeting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GreetingRepository extends JpaRepository<Greeting, Long> {

    Optional<Greeting> findByKey(String key);
}
