package com.finaxys.skillsrh.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(annotations = RestController.class)
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApiException(ApiException exception, HttpServletRequest request) {
        return ApiException.withInstance(exception.getBody(), request.getRequestURI());
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ProblemDetail handleAuthorizationDenied(AuthorizationDeniedException exception, HttpServletRequest request) {
        return problemDetail(
            request,
            HttpStatus.FORBIDDEN,
            "forbidden",
            "You do not have permission to access this resource"
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error -> details.put(error.getField(), error.getDefaultMessage()));
        exception.getBindingResult().getGlobalErrors().forEach(error -> details.put(error.getObjectName(), error.getDefaultMessage()));

        return problemDetail(
            request,
            HttpStatus.UNPROCESSABLE_CONTENT,
            "validation-error",
            "Request validation failed",
            details
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("parameter", exception.getName());
        details.put("value", exception.getValue());

        return problemDetail(
            request,
            HttpStatus.BAD_REQUEST,
            "invalid-request-parameter",
            "Request parameter has an invalid value",
            details
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return problemDetail(
            request,
            HttpStatus.BAD_REQUEST,
            "invalid-request-body",
            "Request body is missing or malformed"
        );
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(Exception exception, HttpServletRequest request) {
        LOGGER.error("Unexpected API error on {}", request.getRequestURI(), exception);
        return problemDetail(
            request,
            HttpStatus.INTERNAL_SERVER_ERROR,
            "internal-server-error",
            "Unexpected API error: " + summarizeException(exception)
        );
    }

    private ProblemDetail problemDetail(HttpServletRequest request, HttpStatus status, String problemTypeId, String detail) {
        return problemDetail(request, status, problemTypeId, detail, null);
    }

    private ProblemDetail problemDetail(HttpServletRequest request, HttpStatus status, String problemTypeId, String detail, Object errors) {
        return ApiException.withInstance(
            ApiException.problemDetail(status, problemTypeId, detail, errors),
            request.getRequestURI()
        );
    }

    private String summarizeException(Exception exception) {
        Throwable current = exception;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            return current.getClass().getSimpleName();
        }
        String singleLine = message.replaceAll("\\s+", " ").trim();
        if (singleLine.length() > 220) {
            singleLine = singleLine.substring(0, 217) + "...";
        }
        return current.getClass().getSimpleName() + " - " + singleLine;
    }
}
