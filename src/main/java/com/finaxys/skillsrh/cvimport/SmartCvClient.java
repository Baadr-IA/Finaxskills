package com.finaxys.skillsrh.cvimport;

import com.finaxys.skillsrh.api.ApiException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class SmartCvClient {

    private final SmartCvProperties properties;
    private final ObjectMapper objectMapper;

    public SmartCvClient(SmartCvProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public SmartCvAnalysisResponse analyze(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "cv-import-file-required", "A CV file is required");
        }

        String boundary = "----FinaxskillsBoundary" + UUID.randomUUID().toString().replace("-", "");

        try {
            byte[] body = buildMultipartBody(file, boundary);
            HttpURLConnection connection = (HttpURLConnection) URI.create(properties.getBaseUrl() + "/analyze?generate_word=false&index=false")
                .toURL()
                .openConnection();
            connection.setRequestMethod(HttpMethod.POST.name());
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            connection.setRequestProperty("Accept", "application/json");
            connection.setFixedLengthStreamingMode(body.length);

            if (StringUtils.hasText(properties.getApiKey())) {
                connection.setRequestProperty("X-API-Key", properties.getApiKey().trim());
            }

            connection.connect();
            try (var outputStream = connection.getOutputStream()) {
                outputStream.write(body);
            }

            int statusCode = connection.getResponseCode();
            String responseBody = readResponseBody(connection, statusCode);
            if (statusCode < 200 || statusCode >= 300) {
                throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "smart-cv-upstream-error",
                    "Smart CV analysis failed: " + extractUpstreamMessage(responseBody, statusCode)
                );
            }

            if (!StringUtils.hasText(responseBody)) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "smart-cv-empty-response", "Smart CV returned an empty response");
            }

            return objectMapper.readValue(responseBody, SmartCvAnalysisResponse.class);
        } catch (ApiException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new ApiException(
                HttpStatus.BAD_GATEWAY,
                "smart-cv-unreachable",
                "Unable to reach Smart CV for CV analysis: " + exception.getMessage()
            );
        }
    }

    private byte[] buildMultipartBody(MultipartFile file, String boundary) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        String lineBreak = "\r\n";
        output.write(("--" + boundary + lineBreak).getBytes(StandardCharsets.UTF_8));
        output.write((
            "Content-Disposition: form-data; name=\"file\"; filename=\"" + escapeQuotes(resolveFilename(file)) + "\"" + lineBreak
        ).getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Type: " + resolveContentType(file.getContentType()) + lineBreak + lineBreak).getBytes(StandardCharsets.UTF_8));
        output.write(file.getBytes());
        output.write(lineBreak.getBytes(StandardCharsets.UTF_8));
        output.write(("--" + boundary + "--" + lineBreak).getBytes(StandardCharsets.UTF_8));
        return output.toByteArray();
    }

    private String escapeQuotes(String value) {
        return value.replace("\"", "");
    }

    private String resolveFilename(MultipartFile file) {
        return StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "cv-upload.bin";
    }

    private String resolveContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return "application/octet-stream";
        }
        return contentType;
    }

    private String readResponseBody(HttpURLConnection connection, int statusCode) throws IOException {
        if (statusCode >= 400) {
            if (connection.getErrorStream() == null) {
                return "";
            }
            try (var inputStream = connection.getErrorStream()) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        try (var inputStream = connection.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String extractUpstreamMessage(String body, int statusCode) {
        try {
            if (StringUtils.hasText(body)) {
                String singleLine = body.replaceAll("\\s+", " ").trim();
                return singleLine.length() > 200 ? singleLine.substring(0, 200) + "..." : singleLine;
            }
        } catch (RuntimeException ignored) {
        }
        return "HTTP " + statusCode;
    }
}
