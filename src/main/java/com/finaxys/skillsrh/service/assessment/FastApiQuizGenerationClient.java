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
import java.util.logging.Logger;

@Service
public class FastApiQuizGenerationClient implements QuizGenerationClient {

    private static final Logger logger = Logger.getLogger(FastApiQuizGenerationClient.class.getName());

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public FastApiQuizGenerationClient(QuizGenerationProperties properties, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.baseUrl = properties.getBaseUrl();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeout());
        requestFactory.setReadTimeout(properties.getReadTimeout());
        logger.info("Initializing FastApiQuizGenerationClient with baseUrl=" + baseUrl + 
            ", connectTimeout=" + properties.getConnectTimeout() + ", readTimeout=" + properties.getReadTimeout());
        this.restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(requestFactory)
            .build();
    }

    @Override
    public QuizModels.GeneratedQuizBlock generateBlock(QuizModels.QuizGenerationRequest request) {
        try {
            logger.info("Calling FastAPI quiz generation: POST " + baseUrl + 
                "/api/v1/quiz-blocks/generate with request: skill=" + request.skill() + 
                ", level=" + request.level() + ", count=" + request.questionCount());
            
            QuizModels.GeneratedQuizBlock block = restClient.post()
                .uri("/api/v1/quiz-blocks/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(QuizModels.GeneratedQuizBlock.class);

            logger.info("FastAPI quiz generation succeeded: " + block.questions().size() + " questions generated");
            validateBlock(block, request.questionCount());
            return block;
        } catch (ResourceAccessException exception) {
            logger.severe("FastAPI quiz generation - connection error (unreachable or timeout): " + exception.getMessage());
            throw new ApiException(HttpStatus.GATEWAY_TIMEOUT, "quiz-api-unreachable",
                "The external quiz API is unreachable or timed out");
        } catch (RestClientResponseException exception) {
            logger.severe("FastAPI quiz generation - HTTP error: status=" + exception.getStatusCode() + 
                ", body=" + exception.getResponseBodyAsString());
            throw mapFastApiError(exception);
        } catch (RestClientException exception) {
            logger.severe("FastAPI quiz generation - unexpected error: " + exception.getMessage());
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
