package com.finaxys.skillsrh.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

import java.net.URI;
import java.util.Locale;

public class ApiException extends ErrorResponseException {

    private static final String PROBLEM_TYPE_BASE_URI = "urn:template-app:problem:";

    public ApiException(HttpStatusCode status, String problemTypeId, String detail) {
        this(status, problemTypeId, detail, null);
    }

    public ApiException(HttpStatusCode status, String problemTypeId, String detail, Object errors) {
        super(status, problemDetail(status, problemTypeId, detail, errors), null);
    }

    public static ProblemDetail problemDetail(HttpStatusCode status, String problemTypeId, String detail) {
        return problemDetail(status, problemTypeId, detail, null);
    }

    public static ProblemDetail problemDetail(HttpStatusCode status, String problemTypeId, String detail, Object errors) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        HttpStatus httpStatus = HttpStatus.resolve(status.value());
        if (httpStatus != null) {
            problemDetail.setTitle(httpStatus.getReasonPhrase());
        }
        problemDetail.setType(problemType(problemTypeId));
        if (errors != null) {
            problemDetail.setProperty("errors", errors);
        }
        return problemDetail;
    }

    public static ProblemDetail withInstance(ProblemDetail problemDetail, String path) {
        if (path != null && !path.isBlank()) {
            problemDetail.setInstance(URI.create(path));
        }
        return problemDetail;
    }

    public static URI problemType(String problemTypeId) {
        return URI.create(PROBLEM_TYPE_BASE_URI + normalize(problemTypeId));
    }

    private static String normalize(String problemTypeId) {
        return problemTypeId
            .trim()
            .toLowerCase(Locale.ROOT)
            .replace('_', '-')
            .replace(' ', '-');
    }
}
