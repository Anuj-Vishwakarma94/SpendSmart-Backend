package com.spendsmart.auth.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET = "my-32-character-ultra-secure-and-ultra-long-secret";
    private static final long EXPIRATION = 1000 * 60 * 60; // 1 hour

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", EXPIRATION);
    }

    @Test
    void generateToken_AndExtractClaims_Success() {
        String token = jwtService.generateToken("test@example.com", 1L, "USER");

        assertNotNull(token);
        assertEquals("test@example.com", jwtService.extractEmail(token));
        assertEquals(1L, jwtService.extractUserId(token));
        assertEquals("USER", jwtService.extractRole(token));
    }

    @Test
    void isTokenValid_ValidToken_ReturnsTrue() {
        String token = jwtService.generateToken("test@example.com", 1L, "USER");
        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    void isTokenValid_InvalidToken_ReturnsFalse() {
        assertFalse(jwtService.isTokenValid("invalid-token-string"));
    }

    @Test
    void isTokenExpired_NotExpired_ReturnsFalse() {
        String token = jwtService.generateToken("test@example.com", 1L, "USER");
        assertFalse(jwtService.isTokenExpired(token));
    }

    @Test
    void isTokenExpired_ExpiredToken_ReturnsTrue() {
        // Set a negative expiration to create an already-expired token
        ReflectionTestUtils.setField(jwtService, "expiration", -1000L); 
        String token = jwtService.generateToken("test@example.com", 1L, "USER");
        
        assertTrue(jwtService.isTokenExpired(token));
        assertFalse(jwtService.isTokenValid(token)); // Valid should also be false
    }
}
