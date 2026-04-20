package com.finaxys.templateappname.controller;

import com.finaxys.templateappname.domain.Greeting;
import com.finaxys.templateappname.repository.GreetingRepository;
import com.finaxys.templateappname.security.permission.Action;
import com.finaxys.templateappname.security.permission.PermissionGrant;
import com.finaxys.templateappname.security.permission.PermissionProfile;
import com.finaxys.templateappname.security.permission.PermissionProfileRepository;
import com.finaxys.templateappname.security.permission.ResourceKey;
import com.finaxys.templateappname.security.permission.Scope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

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

@SpringBootTest(properties = {
    "app.security.oauth2.issuer-uri=https://example.test/realms/template-app",
    "app.security.oauth2.jwk-set-uri=https://example.test/realms/template-app/protocol/openid-connect/certs",
    "app.security.oauth2.audience=template-app-spring-api",
    "app.security.oauth2.api-client-id=template-app-spring-api",
    "app.security.cors.allowed-origins[0]=http://localhost:4200"
})
class GreetingControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @MockitoBean
    private GreetingRepository greetingRepository;

    @MockitoBean
    private PermissionProfileRepository permissionProfileRepository;

    @BeforeEach
    void setUpMockMvcAndPermissionProfiles() {
        this.mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();

        when(permissionProfileRepository.findAllByKey()).thenReturn(Map.of(
            "hr", new PermissionProfile(
                "hr",
                "HR",
                List.of(new PermissionGrant(ResourceKey.GREETINGS, Set.of(Action.READ, Action.CREATE, Action.UPDATE), Scope.ALL))
            ),
            "admin", new PermissionProfile(
                "admin",
                "Admin",
                List.of(new PermissionGrant(ResourceKey.GREETINGS, Set.of(Action.READ, Action.CREATE, Action.UPDATE, Action.DELETE), Scope.ALL))
            ),
            "guest", new PermissionProfile("guest", "Guest", List.of())
        ));
    }

    @Test
    void getGreetingByIdReturns401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/greetings/1"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.type").value("urn:template-app:problem:unauthorized"))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.detail").value("Authentication is required to access this resource"))
            .andExpect(jsonPath("$.instance").value("/api/greetings/1"));
    }

    @Test
    void getGreetingByIdReturns403WhenTokenHasNoRequiredPermission() throws Exception {
        mockMvc.perform(get("/api/greetings/1")
                .with(authenticatedUser("guest-user", "GUEST")))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.type").value("urn:template-app:problem:forbidden"))
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.detail").value("You do not have permission to access this resource"));
    }

    @Test
    void getGreetingByIdReturns200WhenTokenHasRequiredPermission() throws Exception {
        // Given
        Greeting greeting = new Greeting("hello", "Hello World from Spring Boot");
        when(greetingRepository.findById(1L))
            .thenReturn(Optional.of(greeting));

        // When / Then
        mockMvc.perform(get("/api/greetings/1")
                .with(authenticatedUser("hr-user", "HR")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.key").value("hello"))
            .andExpect(jsonPath("$.message").value("Hello World from Spring Boot"));
    }

    @Test
    void getGreetingByIdReturns404WhenGreetingNotFoundAndUserIsAuthorized() throws Exception {
        // Given
        when(greetingRepository.findById(1L)).thenReturn(Optional.empty());

        // When / Then
        mockMvc.perform(get("/api/greetings/1")
                .with(authenticatedUser("hr-user", "HR")))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.type").value("urn:template-app:problem:greeting-not-found"))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.detail").value("Greeting not found"));
    }

    @Test
    void getGreetingByKeyReturns200WhenUserCanRead() throws Exception {
        when(greetingRepository.findByKey("good-night"))
            .thenReturn(Optional.of(new Greeting("good-night", "Good Night World from Spring Boot")));

        mockMvc.perform(get("/api/greetings/key/good-night")
                .with(authenticatedUser("hr-user", "HR")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.key").value("good-night"))
            .andExpect(jsonPath("$.message").value("Good Night World from Spring Boot"));
    }

    @Test
    void getGreetingByKeyReturns404WhenKeyNotFound() throws Exception {
        when(greetingRepository.findByKey("unknown")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/greetings/key/unknown")
            .with(authenticatedUser("hr-user", "HR")))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.type").value("urn:template-app:problem:greeting-not-found"));
    }

    @Test
    void listGreetingsReturns200WhenUserCanRead() throws Exception {
        when(greetingRepository.findAll())
            .thenReturn(List.of(
                new Greeting("welcome", "Welcome"),
                new Greeting("hello", "Hello")
            ));

        mockMvc.perform(get("/api/greetings")
                .with(authenticatedUser("hr-user", "HR")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].key").value("hello"))
            .andExpect(jsonPath("$[1].key").value("welcome"));
    }

    @Test
    void createGreetingReturns201WhenUserCanCreate() throws Exception {
        when(greetingRepository.save(any(Greeting.class)))
            .thenReturn(new Greeting("welcome", "Welcome"));

        mockMvc.perform(post("/api/greetings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"key\":\"welcome\",\"message\":\"Welcome\"}")
                .with(authenticatedUser("hr-user", "HR")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.key").value("welcome"));
    }

    @Test
    void createGreetingReturns422WhenRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/api/greetings")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"key\":\"\",\"message\":\"Welcome\"}")
            .with(authenticatedUser("hr-user", "HR")))
            .andExpect(status().isUnprocessableContent())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.type").value("urn:template-app:problem:validation-error"))
            .andExpect(jsonPath("$.status").value(422))
            .andExpect(jsonPath("$.detail").value("Request validation failed"))
            .andExpect(jsonPath("$.errors.key").value("must not be blank"));
    }

    @Test
    void createGreetingReturns409WhenKeyAlreadyExists() throws Exception {
        when(greetingRepository.save(any(Greeting.class)))
            .thenThrow(new DataIntegrityViolationException("duplicate greeting key"));

        mockMvc.perform(post("/api/greetings")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"key\":\"welcome\",\"message\":\"Welcome\"}")
            .with(authenticatedUser("hr-user", "HR")))
            .andExpect(status().isConflict())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.type").value("urn:template-app:problem:greeting-key-conflict"))
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.detail").value("A greeting with the same key already exists"));
    }

    @Test
    void deleteGreetingReturns403WhenUserCannotDelete() throws Exception {
        mockMvc.perform(delete("/api/greetings/1")
            .with(authenticatedUser("hr-user", "HR")))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.type").value("urn:template-app:problem:forbidden"));
    }

    @Test
    void deleteGreetingReturns204WhenUserCanDelete() throws Exception {
        Greeting greeting = new Greeting("hello", "Hello");
        when(greetingRepository.findById(1L)).thenReturn(Optional.of(greeting));

        mockMvc.perform(delete("/api/greetings/1")
            .with(authenticatedUser("admin-user", "ADMIN")))
            .andExpect(status().isNoContent());
    }

    @Test
    void deleteGreetingReturns204WhenGreetingIsAlreadyMissing() throws Exception {
        when(greetingRepository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/greetings/1")
            .with(authenticatedUser("admin-user", "ADMIN")))
            .andExpect(status().isNoContent());
    }

    private RequestPostProcessor authenticatedUser(String subject, String role) {
        return jwt()
            .jwt(token -> token.subject(subject))
            .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
