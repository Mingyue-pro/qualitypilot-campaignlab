package com.qualitypilot.campaignlab.api;

import com.qualitypilot.campaignlab.campaign.CampaignException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(CampaignException.class)
    ResponseEntity<Map<String, Object>> handleCampaignError(CampaignException error) {
        return ResponseEntity.status(error.getStatus()).body(Map.of(
                "code", error.getCode(),
                "message", error.getMessage(),
                "timestamp", Instant.now().toString()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> handleValidationError(MethodArgumentNotValidException error) {
        String message = error.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .orElse("request validation failed");
        return ResponseEntity.badRequest().body(Map.of(
                "code", "INVALID_REQUEST",
                "message", message,
                "timestamp", Instant.now().toString()
        ));
    }
}
