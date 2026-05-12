package com.finaxys.skillsrh.service.assessment;

import com.finaxys.skillsrh.api.ApiException;
import com.finaxys.skillsrh.config.QuizGenerationProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FastApiQuizGenerationClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void generateBlockReturnsPayloadWhenFastApiSucceeds() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/quiz-blocks/generate", exchange -> writeJson(exchange, 200, """
            {
              "title": "Python - Niveau 3",
              "questions": [
                {"id":"q1","topic":"Lists","targetedOutcome":"Use list APIs","text":"Q1","optionA":"A","optionB":"B","optionC":"C","optionD":"D","explanation":"E1"},
                {"id":"q2","topic":"Lists","targetedOutcome":"Use list APIs","text":"Q2","optionA":"A","optionB":"B","optionC":"C","optionD":"D","explanation":"E2"},
                {"id":"q3","topic":"Lists","targetedOutcome":"Use list APIs","text":"Q3","optionA":"A","optionB":"B","optionC":"C","optionD":"D","explanation":"E3"},
                {"id":"q4","topic":"Lists","targetedOutcome":"Use list APIs","text":"Q4","optionA":"A","optionB":"B","optionC":"C","optionD":"D","explanation":"E4"},
                {"id":"q5","topic":"Lists","targetedOutcome":"Use list APIs","text":"Q5","optionA":"A","optionB":"B","optionC":"C","optionD":"D","explanation":"E5"}
              ],
              "expectedAnswers": [
                {"questionId":"q1","option":"A"},
                {"questionId":"q2","option":"B"},
                {"questionId":"q3","option":"C"},
                {"questionId":"q4","option":"D"},
                {"questionId":"q5","option":"A"}
              ],
              "difficulty": {"level":3,"label":"Intermediate","description":"Desc"},
              "durationMinutes": 15,
              "evaluationCriteria": ["Criterion 1"],
              "generationSource": "fallback",
              "fallbackReason": "LLM unavailable"
            }
            """));
        server.start();

        FastApiQuizGenerationClient client = new FastApiQuizGenerationClient(properties(baseUrl()), new ObjectMapper());

        QuizModels.GeneratedQuizBlock block = client.generateBlock(new QuizModels.QuizGenerationRequest("python", 3, 5, null));

        assertThat(block.questions()).hasSize(5);
        assertThat(block.expectedAnswers()).hasSize(5);
        assertThat(block.generationSource()).isEqualTo("fallback");
    }

    @Test
    void generateBlockMapsFastApiBusinessErrors() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/quiz-blocks/generate", exchange -> writeJson(exchange, 422, """
            {
              "error": {
                "code": "skill-unknown",
                "message": "No referential exists for skill 'go'",
                "details": {}
              }
            }
            """));
        server.start();

        FastApiQuizGenerationClient client = new FastApiQuizGenerationClient(properties(baseUrl()), new ObjectMapper());

        assertThatThrownBy(() -> client.generateBlock(new QuizModels.QuizGenerationRequest("go", 3, 5, null)))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("skill-unknown");
    }

    private QuizGenerationProperties properties(String baseUrl) {
        QuizGenerationProperties properties = new QuizGenerationProperties();
        properties.setBaseUrl(baseUrl);
        properties.setConnectTimeout(Duration.ofSeconds(2));
        properties.setReadTimeout(Duration.ofSeconds(2));
        return properties;
    }

    private String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    private void writeJson(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }
}
