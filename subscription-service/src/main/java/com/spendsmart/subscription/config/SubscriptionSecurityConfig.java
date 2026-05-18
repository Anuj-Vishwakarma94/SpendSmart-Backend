package com.spendsmart.subscription.config;

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
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Key;
import java.util.Date;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SubscriptionSecurityConfig {

    private final SubscriptionJwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/subscription/admin/all").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    System.out.println("[AUTH] 401 Unauthorized for: " + request.getRequestURI() + " - " + authException.getMessage());
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType("application/json");
                    response.getWriter().write("{\"success\":false,\"message\":\"Unauthorized - Invalid or missing token\"}");
                })
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
            "http://localhost:5173",
            "http://localhost:5174",
            "http://127.0.0.1:5173",
            "https://spendsmartpro.duckdns.org"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // ─── JWT Filter ───────────────────────────────────────────
    @Component
    public static class SubscriptionJwtFilter extends OncePerRequestFilter {

        @Value("${jwt.secret}")
        private String secret;

        private Key getKey() {
            return Keys.hmacShaKeyFor(secret.getBytes());
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain chain) throws ServletException, IOException {
            String uri = request.getRequestURI();
            System.out.println("[JWT] Processing request: " + request.getMethod() + " " + uri);
            String header = request.getHeader("Authorization");
            if (header == null || !header.startsWith("Bearer ")) {
                System.out.println("[JWT] No Bearer token found for: " + uri);
                chain.doFilter(request, response);
                return;
            }
            String token = header.substring(7);
            System.out.println("[JWT] Token prefix: " + token.substring(0, Math.min(20, token.length())) + "...");
            try {
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(getKey()).build()
                        .parseClaimsJws(token).getBody();

                System.out.println("[JWT] Claims subject: " + claims.getSubject());
                System.out.println("[JWT] Claims keys: " + claims.keySet());
                System.out.println("[JWT] userId raw type: " + (claims.get("userId") != null ? claims.get("userId").getClass().getName() : "null"));
                System.out.println("[JWT] userId raw value: " + claims.get("userId"));

                if (claims.getExpiration().after(new Date())) {
                    Object userIdRaw = claims.get("userId");
                    Long userId = null;
                    if (userIdRaw instanceof Number) {
                        userId = ((Number) userIdRaw).longValue();
                    } else if (userIdRaw != null) {
                        userId = Long.parseLong(userIdRaw.toString());
                    }
                    String role  = claims.get("role", String.class);
                    String email = claims.getSubject();
                    System.out.println("[JWT] Token valid — email: " + email + ", userId: " + userId + ", role: " + role);

                    if (userId == null) {
                        System.out.println("[JWT] ERROR: userId is null in token claims!");
                        response.setStatus(HttpStatus.UNAUTHORIZED.value());
                        response.setContentType("application/json");
                        response.getWriter().write("{\"success\":false,\"message\":\"Token missing userId claim\"}");
                        return;
                    }

                    var auth = new UsernamePasswordAuthenticationToken(
                            email, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + (role != null ? role : "USER")))
                    );
                    auth.setDetails(userId);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    System.out.println("[JWT] Authentication set successfully for userId: " + userId);
                } else {
                    System.out.println("[JWT] Token expired for: " + uri);
                }
            } catch (ExpiredJwtException e) {
                System.out.println("[JWT] Token expired exception: " + e.getMessage());
            } catch (JwtException e) {
                System.out.println("[JWT] JWT parsing error: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("[JWT] Unexpected exception: " + e.getClass().getName() + " - " + e.getMessage());
                e.printStackTrace();
            }
            chain.doFilter(request, response);
        }
    }
}
