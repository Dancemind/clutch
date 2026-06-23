package com.clutch.app.controller;

import com.clutch.app.exception.QuotaExceededException;
import jakarta.xml.bind.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.net.URI;
import java.time.Instant;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ProblemDetail handleValidationException(ValidationException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Validation error");
        problemDetail.setType(URI.create("https://clutch.io"));
        problemDetail.setProperty("timestamp", Instant.now());
        log.error("Validation error. ", ex);
        return problemDetail;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleSecurityViolation(IllegalStateException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Forbidden");
        problemDetail.setDetail("Security violation.");
        log.error("Security violation. ", ex);
        return problemDetail;
    }

    @ExceptionHandler(QuotaExceededException.class)
    public ProblemDetail handleQuotaException(QuotaExceededException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.PAYMENT_REQUIRED, ex.getMessage());
        problem.setTitle("Forms quota exceeded");
        problem.setProperty("quota_type", "MAX_FORMS");
        log.error("Forms limit error. ", ex);
        return problem;
    }
}
