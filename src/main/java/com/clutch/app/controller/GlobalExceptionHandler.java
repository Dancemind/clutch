package com.clutch.app.controller;

import com.clutch.app.exception.QuotaExceededException;
import jakarta.xml.bind.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.net.URI;
import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ProblemDetail handleValidationException(ValidationException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Ошибка валидации данных");
        problemDetail.setType(URI.create("https://clutch.io"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleSecurityViolation(IllegalStateException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Доступ запрещен");
        problemDetail.setDetail("У вас нет прав для выполнения этой операции в данном контексте.");
        return problemDetail;
    }

    @ExceptionHandler(QuotaExceededException.class)
    public ProblemDetail handleQuotaException(QuotaExceededException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.PAYMENT_REQUIRED, ex.getMessage());
        problem.setTitle("Лимит на создание таблиц/форм превышен");
        problem.setProperty("quota_type", "MAX_FORMS");
        return problem;
    }
}
