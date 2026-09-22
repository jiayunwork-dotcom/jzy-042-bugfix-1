package com.camerageom.api;

import com.camerageom.api.dto.ErrorResponse;
import com.camerageom.validation.ErrorCode;
import com.camerageom.validation.JobValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Maps every failure to a typed, structured error body. Invalid jobs are
 * rejected with HTTP 400 before any computation; nothing is left half-done
 * and no endpoint ever answers an invalid job with an empty result.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(JobValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(JobValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.code().name(), ex.getMessage(), ex.details()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        ErrorCode.MALFORMED_REQUEST.name(),
                        "Request body is missing or not valid JSON for the expected job schema",
                        Map.of()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error while processing request", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        ErrorCode.INTERNAL_ERROR.name(),
                        "Unexpected internal error",
                        Map.of()));
    }
}
