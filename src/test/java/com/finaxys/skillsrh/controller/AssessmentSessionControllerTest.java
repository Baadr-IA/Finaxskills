package com.finaxys.skillsrh.controller;

import com.finaxys.skillsrh.config.DataInitializer;
import com.finaxys.skillsrh.domain.AssessmentSessionStatus;
import com.finaxys.skillsrh.domain.StartLevelSource;
import com.finaxys.skillsrh.repository.AssessmentSessionRepository;
import com.finaxys.skillsrh.repository.CollaboratorRepository;
import com.finaxys.skillsrh.repository.CollaboratorSkillRepository;
import com.finaxys.skillsrh.repository.GreetingRepository;
import com.finaxys.skillsrh.repository.SkillCategoryRepository;
import com.finaxys.skillsrh.repository.SkillRepository;
import com.finaxys.skillsrh.security.permission.Action;
import com.finaxys.skillsrh.security.permission.PermissionGrant;
import com.finaxys.skillsrh.security.permission.PermissionProfile;
import com.finaxys.skillsrh.security.permission.PermissionProfileRepository;
import com.finaxys.skillsrh.security.permission.ResourceKey;
import com.finaxys.skillsrh.security.permission.Scope;
import com.finaxys.skillsrh.service.assessment.AdaptiveAssessmentSessionService;
import com.finaxys.skillsrh.service.assessment.QuizModels;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.security.oauth2.issuer-uri=https://example.test/realms/skills-rh",
    "app.security.oauth2.jwk-set-uri=https://example.test/realms/skills-rh/protocol/openid-connect/certs",
    "app.security.oauth2.audience=skills-rh-api",
    "app.security.oauth2.api-client-id=skills-rh-api",
    "app.security.cors.allowed-origins[0]=http://localhost:4200"
})
class AssessmentSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DataInitializer dataInitializer;

    @MockitoBean
    private AdaptiveAssessmentSessionService assessmentSessionService;

    @MockitoBean
    private CollaboratorRepository collaboratorRepository;

    @MockitoBean
    private CollaboratorSkillRepository collaboratorSkillRepository;

    @MockitoBean
    private SkillRepository skillRepository;

    @MockitoBean
    private SkillCategoryRepository skillCategoryRepository;

    @MockitoBean
    private GreetingRepository greetingRepository;

    @MockitoBean
    private AssessmentSessionRepository assessmentSessionRepository;

    @MockitoBean
    private PermissionProfileRepository permissionProfileRepository;

    @BeforeEach
    void setUp() {
        when(permissionProfileRepository.findAllByKey()).thenReturn(Map.of(
            "hr", new PermissionProfile(
                "hr",
                "HR",
                List.of(new PermissionGrant(ResourceKey.SKILL_ASSESSMENTS, Set.of(Action.READ, Action.CREATE, Action.UPDATE), Scope.ALL))
            ),
            "guest", new PermissionProfile("guest", "Guest", List.of())
        ));
    }

    @Test
    void startSessionReturnsCreatedWithoutExpectedAnswers() throws Exception {
        when(assessmentSessionService.startSession(any())).thenReturn(sessionState());

        mockMvc.perform(post("/api/assessment-sessions")
                .contentType("application/json")
                .content("""
                    {
                      "collaboratorId": 1,
                      "skillId": 10,
                      "startLevelSource": "HR",
                      "instructions": "Focus APIs"
                    }
                    """)
                .with(authenticatedUser("hr-user", "HR")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.skillName").value("Python"))
            .andExpect(jsonPath("$.blocks[0].questions[0].text").value("What does list.append do?"))
            .andExpect(jsonPath("$.blocks[0].generationSource").value("llm"))
            .andExpect(jsonPath("$.blocks[0].expectedAnswers").doesNotExist());
    }

    @Test
    void getSessionReturnsForbiddenWithoutPermission() throws Exception {
        mockMvc.perform(get("/api/assessment-sessions/99")
                .with(authenticatedUser("guest-user", "GUEST")))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    private QuizModels.AssessmentSessionState sessionState() {
        return new QuizModels.AssessmentSessionState(
            99L,
            1L,
            "Alice Martin",
            10L,
            "Python",
            "python",
            StartLevelSource.HR,
            3,
            3,
            5,
            5,
            "Focus APIs",
            AssessmentSessionStatus.IN_PROGRESS,
            Instant.parse("2026-05-12T09:00:00Z"),
            Instant.parse("2026-05-12T09:00:00Z"),
            List.of(
                new QuizModels.SessionBlock(
                    1,
                    3,
                    "Python - Niveau 3",
                    List.of(
                        new QuizModels.GeneratedQuestion(
                            "q1",
                            "Lists",
                            "Use common list APIs",
                            "What does list.append do?",
                            "Removes the last item",
                            "Adds an item to the end",
                            "Sorts the list",
                            "Returns the length",
                            "append adds an item at the end"
                        )
                    ),
                    List.of(new QuizModels.ExpectedAnswer("q1", "B")),
                    new QuizModels.Difficulty(3, "Intermediate", "Description"),
                    15,
                    List.of("Criterion 1"),
                    "llm",
                    null,
                    QuizModels.SessionBlockStatus.ACTIVE,
                    List.of(),
                    null,
                    null,
                    null
                )
            )
        );
    }

    private RequestPostProcessor authenticatedUser(String subject, String role) {
        return jwt()
            .jwt(token -> token.subject(subject))
            .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
