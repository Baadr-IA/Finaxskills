package com.finaxys.skillsrh.controller;

import com.finaxys.skillsrh.config.DataInitializer;
import com.finaxys.skillsrh.domain.Collaborator;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
class CollaboratorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DataInitializer dataInitializer;

    @MockitoBean
    private CollaboratorRepository collaboratorRepository;

    @MockitoBean
    private CollaboratorSkillRepository collaboratorSkillRepository;

    @MockitoBean
    private SkillCategoryRepository skillCategoryRepository;

    @MockitoBean
    private SkillRepository skillRepository;

    @MockitoBean
    private GreetingRepository greetingRepository;

    @MockitoBean
    private PermissionProfileRepository permissionProfileRepository;

    @BeforeEach
    void setUp() {
        when(permissionProfileRepository.findAllByKey()).thenReturn(Map.of(
            "hr", new PermissionProfile(
                "hr",
                "HR",
                List.of(new PermissionGrant(ResourceKey.COLLABORATORS, Set.of(Action.READ, Action.CREATE, Action.UPDATE), Scope.ALL))
            ),
            "admin", new PermissionProfile(
                "admin",
                "Admin",
                List.of(new PermissionGrant(ResourceKey.COLLABORATORS, Set.of(Action.READ, Action.CREATE, Action.UPDATE, Action.DELETE), Scope.ALL))
            ),
            "guest", new PermissionProfile("guest", "Guest", List.of())
        ));
    }

    @Test
    void listCollaboratorsReturns401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/collaborators"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void listCollaboratorsReturns403WhenTokenHasNoPermission() throws Exception {
        mockMvc.perform(get("/api/collaborators")
                .with(authenticatedUser("guest-user", "GUEST")))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void listCollaboratorsReturns200WhenAuthorized() throws Exception {
        Collaborator alice = new Collaborator("Alice", "Martin", "alice@test.com", "Dev", null);
        when(collaboratorRepository.findAll()).thenReturn(List.of(alice));

        mockMvc.perform(get("/api/collaborators")
                .with(authenticatedUser("hr-user", "HR")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].firstName").value("Alice"))
            .andExpect(jsonPath("$[0].lastName").value("Martin"));
    }

    @Test
    void getCollaboratorByIdReturns404WhenNotFound() throws Exception {
        when(collaboratorRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/collaborators/99")
                .with(authenticatedUser("hr-user", "HR")))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.type").value("urn:template-app:problem:collaborator-not-found"));
    }

    @Test
    void getCollaboratorByIdReturns200WhenFound() throws Exception {
        Collaborator bob = new Collaborator("Bob", "Dupont", "bob@test.com", "Dev", "kc-bob");
        when(collaboratorRepository.findById(1L)).thenReturn(Optional.of(bob));

        mockMvc.perform(get("/api/collaborators/1")
                .with(authenticatedUser("hr-user", "HR")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("Bob"))
            .andExpect(jsonPath("$.email").value("bob@test.com"));
    }

    @Test
    void createCollaboratorReturns201WhenAuthorized() throws Exception {
        Collaborator saved = new Collaborator("Claire", "Leroy", "claire@test.com", "DevOps", null);
        when(collaboratorRepository.save(any(Collaborator.class))).thenReturn(saved);

        mockMvc.perform(post("/api/collaborators")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Claire\",\"lastName\":\"Leroy\",\"email\":\"claire@test.com\",\"jobTitle\":\"DevOps\"}")
                .with(authenticatedUser("hr-user", "HR")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.firstName").value("Claire"));
    }

    @Test
    void createCollaboratorReturns422WhenRequestInvalid() throws Exception {
        mockMvc.perform(post("/api/collaborators")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"\",\"lastName\":\"Leroy\",\"email\":\"claire@test.com\"}")
                .with(authenticatedUser("hr-user", "HR")))
            .andExpect(status().isUnprocessableContent())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.type").value("urn:template-app:problem:validation-error"))
            .andExpect(jsonPath("$.errors.firstName").exists());
    }

    @Test
    void createCollaboratorReturns409WhenEmailAlreadyExists() throws Exception {
        when(collaboratorRepository.save(any(Collaborator.class)))
            .thenThrow(new DataIntegrityViolationException("duplicate email"));

        mockMvc.perform(post("/api/collaborators")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Alice\",\"lastName\":\"Martin\",\"email\":\"alice@test.com\"}")
                .with(authenticatedUser("hr-user", "HR")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.type").value("urn:template-app:problem:collaborator-email-conflict"));
    }

    @Test
    void deleteCollaboratorReturns403WhenRoleIsHr() throws Exception {
        mockMvc.perform(delete("/api/collaborators/1")
                .with(authenticatedUser("hr-user", "HR")))
            .andExpect(status().isForbidden());
    }

    @Test
    void deleteCollaboratorReturns204WhenAdminDeletes() throws Exception {
        Collaborator alice = new Collaborator("Alice", "Martin", "alice@test.com", null, null);
        when(collaboratorRepository.findById(1L)).thenReturn(Optional.of(alice));

        mockMvc.perform(delete("/api/collaborators/1")
                .with(authenticatedUser("admin-user", "ADMIN")))
            .andExpect(status().isNoContent());
    }

    private RequestPostProcessor authenticatedUser(String subject, String role) {
        return jwt()
            .jwt(token -> token.subject(subject))
            .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
