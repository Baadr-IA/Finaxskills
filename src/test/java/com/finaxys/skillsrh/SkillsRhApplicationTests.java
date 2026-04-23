package com.finaxys.skillsrh;

import com.finaxys.skillsrh.config.DataInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@org.testcontainers.junit.jupiter.Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {
    "app.security.oauth2.issuer-uri=https://fake-issuer.test",
    "app.security.oauth2.jwk-set-uri=https://fake-issuer.test/jwks",
    "app.security.oauth2.audience=fake-audience",
    "app.security.oauth2.api-client-id=fake-client"
})
class SkillsRhApplicationTests {

	@MockitoBean
	DataInitializer dataInitializer;

	@Test
	void contextLoads() {
	}

}

