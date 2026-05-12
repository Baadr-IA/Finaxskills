package com.finaxys.skillsrh.service.assessment;

import com.finaxys.skillsrh.api.ApiException;
import com.finaxys.skillsrh.config.QuizGenerationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Service
public class FastApiQuizGenerationClient implements QuizGenerationClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public FastApiQuizGenerationClient(QuizGenerationProperties properties, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeout());
        requestFactory.setReadTimeout(properties.getReadTimeout());
        this.restClient = RestClient.builder()
            .baseUrl(properties.getBaseUrl())
            .requestFactory(requestFactory)
            .build();
    }

    @Override
    public QuizModels.GeneratedQuizBlock generateBlock(QuizModels.QuizGenerationRequest request) {
        try {
            QuizModels.GeneratedQuizBlock block = restClient.post()
                .uri("/api/v1/quiz-blocks/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(QuizModels.GeneratedQuizBlock.class);

            validateBlock(block, request.questionCount());
            return block;
        } catch (ResourceAccessException exception) {
            throw new ApiException(HttpStatus.GATEWAY_TIMEOUT, "quiz-api-unreachable",
                "The external quiz API is unreachable or timed out");
        } catch (RestClientResponseException exception) {
            throw mapFastApiError(exception);
        } catch (RestClientException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "quiz-api-error",
                "The external quiz API call failed");
        }
    }

    private void validateBlock(QuizModels.GeneratedQuizBlock block, int expectedQuestionCount) {
        if (block == null || block.questions() == null || block.expectedAnswers() == null || block.difficulty() == null) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "quiz-api-invalid-payload",
                "The external quiz API returned an incomplete payload");
        }
        if (block.questions().size() != expectedQuestionCount || block.expectedAnswers().size() != expectedQuestionCount) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "quiz-api-invalid-payload",
                "The external quiz API returned an unexpected number of questions");
        }
    }

    private ApiException mapFastApiError(RestClientResponseException exception) {
        String body = exception.getResponseBodyAsString();
        if (body != null && !body.isBlank()) {
            try {
                Map<String, Object> payload = objectMapper.readValue(body, new TypeReference<>() {});
                Object errorNode = payload.get("error");
                if (errorNode instanceof Map<?, ?> errorMap) {
                    String code = String.valueOf(errorMap.containsKey("code") ? errorMap.get("code") : "quiz-api-error");
                    String message = String.valueOf(errorMap.containsKey("message") ? errorMap.get("message") : "The external quiz API returned an error");
                    return new ApiException(mapStatus(code), code, message);
                }
            } catch (Exception ignored) {
                // Fall through to default mapping.
            }
        }

        return new ApiException(HttpStatus.BAD_GATEWAY, "quiz-api-error",
            "The external quiz API returned an error");
    }

    private HttpStatus mapStatus(String code) {
        return switch (code) {
            case "skill-unknown", "invalid-level", "invalid-request" -> HttpStatus.UNPROCESSABLE_CONTENT;
            case "generator-timeout" -> HttpStatus.GATEWAY_TIMEOUT;
            default -> HttpStatus.BAD_GATEWAY;
        };
    }
}
