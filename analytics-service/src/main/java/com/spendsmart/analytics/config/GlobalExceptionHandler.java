package com.spendsmart.analytics.config;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String,Object>> rt(RuntimeException e) { return err(HttpStatus.NOT_FOUND, e.getMessage()); }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,Object>> ia(IllegalArgumentException e) { return err(HttpStatus.BAD_REQUEST, e.getMessage()); }
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String,Object>> se(SecurityException e) { return err(HttpStatus.FORBIDDEN, e.getMessage()); }
    private ResponseEntity<Map<String,Object>> err(HttpStatus s, String m) {
        return ResponseEntity.status(s).body(Map.of("timestamp", LocalDateTime.now().toString(), "status", s.value(), "message", String.valueOf(m)));
    }
}
