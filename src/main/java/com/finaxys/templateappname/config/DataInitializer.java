package com.finaxys.templateappname.config;

import com.finaxys.templateappname.domain.Greeting;
import com.finaxys.templateappname.repository.GreetingRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements ApplicationRunner {

    private final GreetingRepository greetingRepository;

    public DataInitializer(GreetingRepository greetingRepository) {
        this.greetingRepository = greetingRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (greetingRepository.count() == 0) {
            greetingRepository.saveAll(List.of(
                new Greeting("hello", "Hello World from DB"),
                new Greeting("good-night", "Good Night World from DB")
            ));
        }
    }
}
