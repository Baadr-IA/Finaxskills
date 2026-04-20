package com.finaxys.templateappname.controller;

import com.finaxys.templateappname.api.ApiException;
import com.finaxys.templateappname.domain.Greeting;
import com.finaxys.templateappname.repository.GreetingRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class GreetingController {

    private final GreetingRepository greetingRepository;

    public GreetingController(GreetingRepository greetingRepository) {
        this.greetingRepository = greetingRepository;
    }

    @GetMapping("/greetings")
    @PreAuthorize("@permissions.has(authentication, 'GREETINGS', 'READ', 'ALL')")
    public List<GreetingResponse> list() {
        return greetingRepository.findAll().stream()
            .sorted((left, right) -> left.getKey().compareToIgnoreCase(right.getKey()))
            .map(greeting -> new GreetingResponse(greeting.getId(), greeting.getKey(), greeting.getMessage()))
            .toList();
    }

    @GetMapping("/greetings/{id}")
    @PreAuthorize("@permissions.has(authentication, 'GREETINGS', 'READ', 'ALL')")
    public GreetingResponse getById(@PathVariable Long id) {
        return toGreetingResponse(requireGreeting(id));
    }

    @GetMapping("/greetings/key/{key}")
    @PreAuthorize("@permissions.has(authentication, 'GREETINGS', 'READ', 'ALL')")
    public GreetingResponse getByKey(@PathVariable String key) {
        return toGreetingResponse(requireGreeting(key));
    }

    @PostMapping("/greetings")
    @PreAuthorize("@permissions.has(authentication, 'GREETINGS', 'CREATE', 'ALL')")
    public ResponseEntity<GreetingResponse> create(@Valid @RequestBody CreateGreetingRequest request) {
        Greeting saved = saveGreeting(new Greeting(request.key(), request.message()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toGreetingResponse(saved));
    }

    @PutMapping("/greetings/{id}")
    @PreAuthorize("@permissions.has(authentication, 'GREETINGS', 'UPDATE', 'ALL')")
    public GreetingResponse update(@PathVariable Long id, @Valid @RequestBody UpdateGreetingRequest request) {
        Greeting greeting = requireGreeting(id);

        greeting.setKey(request.key());
        greeting.setMessage(request.message());

        return toGreetingResponse(saveGreeting(greeting));
    }

    @DeleteMapping("/greetings/{id}")
    @PreAuthorize("@permissions.has(authentication, 'GREETINGS', 'DELETE', 'ALL')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        greetingRepository.findById(id).ifPresent(greetingRepository::delete);
        return ResponseEntity.noContent().build();
    }

    private Greeting requireGreeting(Long id) {
        return greetingRepository.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "greeting-not-found", "Greeting not found"));
    }

    private Greeting requireGreeting(String key) {
        return greetingRepository.findByKey(key)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "greeting-not-found", "Greeting not found"));
    }

    private Greeting saveGreeting(Greeting greeting) {
        try {
            return greetingRepository.save(greeting);
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "greeting-key-conflict", "A greeting with the same key already exists");
        }
    }

    private GreetingResponse toGreetingResponse(Greeting greeting) {
        return new GreetingResponse(greeting.getId(), greeting.getKey(), greeting.getMessage());
    }

    public record GreetingResponse(Long id, String key, String message) {
    }

    public record CreateGreetingRequest(
        @NotBlank @Size(max = 50) String key,
        @NotBlank @Size(max = 255) String message
    ) {
    }

    public record UpdateGreetingRequest(
        @NotBlank @Size(max = 50) String key,
        @NotBlank @Size(max = 255) String message
    ) {
    }
}
