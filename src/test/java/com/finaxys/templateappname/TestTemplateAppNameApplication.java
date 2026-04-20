package com.finaxys.templateappname;

import org.springframework.boot.SpringApplication;

public class TestTemplateAppNameApplication {

	public static void main(String[] args) {
		SpringApplication.from(TemplateAppNameApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
