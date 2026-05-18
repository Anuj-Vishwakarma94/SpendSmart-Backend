package com.spendsmart.budget.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Key;
import java.util.Map;

/**
 * PremiumCheckFilter — blocks free-tier users from accessing budget-service.
 * Calls subscription-service to verify active Premium plan.
 * Returns HTTP 403 + PREMIUM_REQUIRED error so frontend shows upgrade prompt.
 */
@Component
@Slf4j
public class PremiumCheckFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${subscription.service.url:http://localhost:8090}")
    private String subscriptionServiceUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Key getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())
                || request.getRequestURI().startsWith("/actuator")) {
            chain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getKey()).build()
                    .parseClaimsJws(token).getBody();

            Object userIdRaw = claims.get("userId");
            Long userId = null;
            if (userIdRaw instanceof Number) {
                userId = ((Number) userIdRaw).longValue();
            } else if (userIdRaw != null) {
                userId = Long.parseLong(userIdRaw.toString());
            }
            boolean isPremium = checkPremiumStatus(userId, header);

            if (!isPremium) {
                log.info("Access denied to budget-service for userId={}: not Premium", userId);
                sendUpgradeRequired(response);
                return;
            }

        } catch (Exception e) {
            log.error("PremiumCheckFilter error: {}", e.getMessage());
        }

        chain.doFilter(request, response);
    }

    private boolean checkPremiumStatus(Long userId, String authHeader) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authHeader);
            headers.set("X-User-Id", userId.toString());
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            var result = restTemplate.exchange(
                    subscriptionServiceUrl + "/api/subscription/is-premium",
                    HttpMethod.GET, entity, Boolean.class);
            return Boolean.TRUE.equals(result.getBody());
        } catch (Exception e) {
            log.error("Failed to reach subscription-service: {}", e.getMessage());
            return false;
        }
    }

    private void sendUpgradeRequired(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = Map.of(
                "success", false,
                "status", 403,
                "error", "PREMIUM_REQUIRED",
                "message", "This feature requires a SpendSmart Premium subscription (\u20b9199/month). Upgrade to access it.",
                "upgradeUrl", "/api/subscription/checkout"
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
