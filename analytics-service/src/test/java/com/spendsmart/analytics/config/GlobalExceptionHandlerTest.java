package com.spendsmart.analytics.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handleRuntimeException: returns 404 NOT_FOUND with message")
    void handleRuntimeException_returns404() {
        RuntimeException ex = new RuntimeException("Resource not found");

        ResponseEntity<Map<String, Object>> response = handler.rt(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsKey("timestamp");
        assertThat(response.getBody()).containsEntry("status", 404);
        assertThat(response.getBody()).containsEntry("message", "Resource not found");
    }

    @Test
    @DisplayName("handleRuntimeException: null message is represented as 'null' string")
    void handleRuntimeException_nullMessage() {
        RuntimeException ex = new RuntimeException((String) null);

        ResponseEntity<Map<String, Object>> response = handler.rt(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("message", "null");
    }

    @Test
    @DisplayName("handleIllegalArgumentException: returns 400 BAD_REQUEST with message")
    void handleIllegalArgumentException_returns400() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid parameter");

        ResponseEntity<Map<String, Object>> response = handler.ia(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsKey("timestamp");
        assertThat(response.getBody()).containsEntry("status", 400);
        assertThat(response.getBody()).containsEntry("message", "Invalid parameter");
    }

    @Test
    @DisplayName("handleSecurityException: returns 403 FORBIDDEN with message")
    void handleSecurityException_returns403() {
        SecurityException ex = new SecurityException("Access denied");

        ResponseEntity<Map<String, Object>> response = handler.se(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsKey("timestamp");
        assertThat(response.getBody()).containsEntry("status", 403);
        assertThat(response.getBody()).containsEntry("message", "Access denied");
    }

    @Test
    @DisplayName("error response body always contains timestamp key")
    void errorResponse_alwaysHasTimestamp() {
        ResponseEntity<Map<String, Object>> response = handler.rt(new RuntimeException("test"));

        assertThat(response.getBody()).containsKey("timestamp");
        assertThat(response.getBody().get("timestamp")).isNotNull();
        assertThat(response.getBody().get("timestamp").toString()).isNotBlank();
    }
}
