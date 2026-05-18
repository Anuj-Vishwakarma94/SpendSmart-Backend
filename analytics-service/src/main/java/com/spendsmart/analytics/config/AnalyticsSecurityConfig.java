package com.spendsmart.analytics.config;

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
import org.springframework.http.HttpMethod;
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
import org.springframework.web.cors.*;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Key;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration @EnableWebSecurity @RequiredArgsConstructor
public class AnalyticsSecurityConfig {

    private final PremiumCheckFilter premiumCheckFilter;

    private final AnalyticsJwtFilter jwtFilter;

    @Bean public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .cors(c -> c.configurationSource(cors()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.OPTIONS,"/**").permitAll()
                .requestMatchers("/actuator/**").permitAll().anyRequest().authenticated())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(premiumCheckFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean public CorsConfigurationSource cors() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(List.of(
            "http://localhost:5173",
            "http://localhost:5174",
            "http://127.0.0.1:5173",
            "https://spendsmartpro.duckdns.org"
        ));
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        c.setAllowedHeaders(List.of("*")); c.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
        s.registerCorsConfiguration("/**", c); return s;
    }

    @Bean public org.springframework.web.client.RestTemplate restTemplate() {
        org.springframework.web.client.RestTemplate template = new org.springframework.web.client.RestTemplate();
        template.getInterceptors().add((request, body, execution) -> {
            org.springframework.web.context.request.ServletRequestAttributes attributes = 
                (org.springframework.web.context.request.ServletRequestAttributes) 
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                String authHeader = attributes.getRequest().getHeader("Authorization");
                if (authHeader != null) {
                    request.getHeaders().add("Authorization", authHeader);
                }
            }
            return execution.execute(request, body);
        });
        return template;
    }

    @Component
    public static class AnalyticsJwtFilter extends OncePerRequestFilter {

        private static final Logger log = LoggerFactory.getLogger(AnalyticsJwtFilter.class);

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
                } catch (JwtException e) {
                    log.debug("Invalid JWT token — request will proceed unauthenticated: {}", e.getMessage());
                }
            }
            chain.doFilter(req, res);
        }
    }
}
