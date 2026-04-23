package com.finaxys.skillsrh.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(annotations = RestController.class)
public class ApiExceptionHandler {

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
        return problemDetail(
            request,
            HttpStatus.INTERNAL_SERVER_ERROR,
            "internal-server-error",
            "An unexpected error occurred"
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
}
