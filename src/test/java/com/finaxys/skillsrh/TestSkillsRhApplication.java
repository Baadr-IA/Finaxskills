package com.finaxys.skillsrh;

import org.springframework.boot.SpringApplication;

public class TestSkillsRhApplication {

	public static void main(String[] args) {
		SpringApplication.from(SkillsRhApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}

