package com.spendsmart.notification.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.cors.*;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Key;
import java.time.LocalDateTime;
import java.util.*;

@Configuration @EnableWebSecurity @RequiredArgsConstructor
public class NotifSecurityConfig {

    private final NotifJwtFilter jwtFilter;

    @Bean public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .cors(c -> c.configurationSource(cors()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Allow internal service calls (budget-alert, bulk) without user token
                .requestMatchers("/api/notifications/budget-alert").permitAll()
                .requestMatchers("/api/notifications/bulk").permitAll()
                .requestMatchers("/actuator/**").permitAll().anyRequest().authenticated())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean public CorsConfigurationSource cors() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(List.of("http://localhost:5173"));
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("*")); c.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
        s.registerCorsConfiguration("/**", c); return s;
    }

    @Component
    public static class NotifJwtFilter extends OncePerRequestFilter {
        @Value("${jwt.secret}") private String secret;
        private Key key() { return Keys.hmacShaKeyFor(secret.getBytes()); }

        @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                throws ServletException, IOException {
            String h = req.getHeader("Authorization");
            if (h != null && h.startsWith("Bearer ")) {
                try {
                    Claims c = Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(h.substring(7)).getBody();
                    if (c.getExpiration().after(new Date())) {
                        var auth = new UsernamePasswordAuthenticationToken(c.getSubject(), null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + c.get("role", String.class))));
                        Object userIdRaw = c.get("userId");
                        Long userId = null;
                        if (userIdRaw instanceof Number) {
                            userId = ((Number) userIdRaw).longValue();
                        } else if (userIdRaw != null) {
                            userId = Long.parseLong(userIdRaw.toString());
                        }
                        auth.setDetails(userId);
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                } catch (JwtException ignored) {}
            }
            chain.doFilter(req, res);
        }
    }
}

@RestControllerAdvice
class NotifExceptionHandler {
    @ExceptionHandler(RuntimeException.class)
    ResponseEntity<Map<String, Object>> rt(RuntimeException e) { return err(HttpStatus.NOT_FOUND, e.getMessage()); }
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, Object>> ia(IllegalArgumentException e) { return err(HttpStatus.BAD_REQUEST, e.getMessage()); }
    @ExceptionHandler(SecurityException.class)
    ResponseEntity<Map<String, Object>> se(SecurityException e) { return err(HttpStatus.FORBIDDEN, e.getMessage()); }

    private ResponseEntity<Map<String, Object>> err(HttpStatus s, String m) {
        return ResponseEntity.status(s).body(Map.of(
                "timestamp", LocalDateTime.now().toString(), "status", s.value(), "message", String.valueOf(m)));
    }
}
