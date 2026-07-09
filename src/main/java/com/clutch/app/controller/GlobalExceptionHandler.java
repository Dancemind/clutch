package com.clutch.app.controller;

import com.clutch.app.exceptions.BadCredentialsException;
import com.clutch.app.exceptions.IllegalArgumentException;
import com.clutch.app.exceptions.QuotaExceededException;
import com.clutch.app.exceptions.ResourceNotFoundException;
import com.clutch.app.exceptions.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            ValidationException.class,
            com.clutch.app.exceptions.ValidationException.class,
            IllegalArgumentException.class,
            ResourceNotFoundException.class
    })
    public ResponseEntity<ProblemDetail> handleValidationException(Exception ex, HttpServletRequest request) {
        return mapToResponseEntity(request, HttpStatus.BAD_REQUEST, ex);
    }


    @ExceptionHandler({
            UserNotFoundException.class,
            BadCredentialsException.class
    })
    public ResponseEntity<ProblemDetail> handleValidation(RuntimeException ex, HttpServletRequest request) {
        return mapToResponseEntity(request, HttpStatus.UNAUTHORIZED, ex);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleSecurityViolation(IllegalStateException ex, HttpServletRequest request) {
        return mapToResponseEntity(request, HttpStatus.FORBIDDEN, ex);

    }

    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<ProblemDetail> handleQuotaException(QuotaExceededException ex, HttpServletRequest request) {
        return mapToResponseEntity(request, HttpStatus.PAYMENT_REQUIRED, ex);
    }

    private ResponseEntity<ProblemDetail> mapToResponseEntity(@NotNull HttpServletRequest request,
                                                              @NotNull HttpStatus httpStatus,
                                                              @NotNull Exception ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(httpStatus, ex.getMessage());
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now().toString());

        return ResponseEntity.status(httpStatus).body(problemDetail);
    }

}
