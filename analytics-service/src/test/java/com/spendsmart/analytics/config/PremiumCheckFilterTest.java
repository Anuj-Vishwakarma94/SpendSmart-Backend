package com.spendsmart.analytics.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PremiumCheckFilterTest {

    @InjectMocks
    private PremiumCheckFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private String validToken;
    private final String secret = "testSecretKey12345678901234567890AB";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(filter, "secret", secret);
        ReflectionTestUtils.setField(filter, "subscriptionServiceUrl", "http://localhost:8090");

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 1L);
        claims.put("role", "USER");

        validToken = Jwts.builder()
                .setClaims(claims)
                .setSubject("user@test.com")
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    @DisplayName("doFilterInternal: passes OPTIONS requests without checking")
    void passesOptionsRequests() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("OPTIONS");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal: passes /actuator requests without checking")
    void passesActuatorRequests() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/actuator/health");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal: passes if no Authorization header")
    void passesIfNoAuthHeader() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/analytics");
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal: blocks non-premium users")
    void blocksNonPremiumUsers() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/analytics");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    when(mock.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Boolean.class)))
                            .thenReturn(new ResponseEntity<>(Boolean.FALSE, HttpStatus.OK));
                })) {
            
            filter.doFilterInternal(request, response, filterChain);

            verify(response).setStatus(HttpStatus.FORBIDDEN.value());
            verify(response).setContentType("application/json");
            assertThat(stringWriter.toString()).contains("PREMIUM_REQUIRED");
            verify(filterChain, never()).doFilter(any(), any());
        }
    }

    @Test
    @DisplayName("doFilterInternal: passes premium users")
    void passesPremiumUsers() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/analytics");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    when(mock.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Boolean.class)))
                            .thenReturn(new ResponseEntity<>(Boolean.TRUE, HttpStatus.OK));
                })) {

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }
    }

    @Test
    @DisplayName("doFilterInternal: blocks on RestTemplate exception")
    void blocksOnRestTemplateException() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/analytics");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, context) -> {
                    when(mock.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Boolean.class)))
                            .thenThrow(new RestClientException("Connection refused"));
                })) {

            filter.doFilterInternal(request, response, filterChain);

            verify(response).setStatus(HttpStatus.FORBIDDEN.value());
            verify(response).setContentType("application/json");
            verify(filterChain, never()).doFilter(any(), any());
        }
    }

    @Test
    @DisplayName("doFilterInternal: passes on invalid token")
    void passesOnInvalidToken() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/analytics");
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid_token");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
