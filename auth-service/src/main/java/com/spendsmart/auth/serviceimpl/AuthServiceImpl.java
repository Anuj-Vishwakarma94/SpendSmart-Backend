package com.spendsmart.auth.serviceimpl;

import com.spendsmart.auth.config.JwtService;
import com.spendsmart.auth.dto.AuthDto;
import com.spendsmart.auth.entity.PasswordResetToken;
import com.spendsmart.auth.entity.User;
import com.spendsmart.auth.repository.PasswordResetTokenRepository;
import com.spendsmart.auth.repository.UserRepository;
import com.spendsmart.auth.service.AuthService;
import com.spendsmart.auth.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service

@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final PasswordResetTokenRepository resetTokenRepository;

    @Value("${app.password-reset.expiry-minutes:30}")
    private int resetExpiryMinutes;

    @org.springframework.beans.factory.annotation.Value("${google.client.id}")
    private String googleClientId;

    private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();
    private java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();

    @Override
    @Transactional
    public AuthDto.AuthResponse register(AuthDto.RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .currency(request.getCurrency())
                .timezone(request.getTimezone())
                .provider(User.AuthProvider.LOCAL)
                .isActive(true)
                .role(User.Role.USER)
                .build();

        User saved = userRepository.save(user);
        String token = jwtService.generateToken(saved.getEmail(), saved.getUserId(), saved.getRole().name());
        
        // Send async login notification email
        emailService.sendLoginNotification(saved.getEmail(), saved.getFullName());
        
        return new AuthDto.AuthResponse(token, toUserResponse(saved));
    }

    @Override
    public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalStateException("Account is deactivated");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getUserId(), user.getRole().name());

        // Send async login notification email
        emailService.sendLoginNotification(user.getEmail(), user.getFullName());

        return new AuthDto.AuthResponse(token, toUserResponse(user));
    }

    @Override
    public AuthDto.MessageResponse logout(String token) {
        // Stateless JWT — client discards token; can add blacklist via Redis later
        return new AuthDto.MessageResponse("Logged out successfully", true);
    }

    @Override
    public boolean validateToken(String token) {
        return jwtService.isTokenValid(token) && !jwtService.isTokenExpired(token);
    }

    @Override
    public String refreshToken(String token) {
        if (!jwtService.isTokenValid(token)) {
            throw new IllegalArgumentException("Invalid token");
        }
        String email = jwtService.extractEmail(token);
        Long userId = jwtService.extractUserId(token);
        String role = jwtService.extractRole(token);
        return jwtService.generateToken(email, userId, role);
    }

    @Override
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    @Override
    @Transactional
    public AuthDto.UserResponse updateProfile(Long userId, AuthDto.UpdateProfileRequest request) {
        User user = getUserById(userId);
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getTimezone() != null) user.setTimezone(request.getTimezone());
        return toUserResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public AuthDto.MessageResponse changePassword(Long userId, AuthDto.ChangePasswordRequest request) {
        User user = getUserById(userId);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return new AuthDto.MessageResponse("Password changed successfully", true);
    }

    @Override
    @Transactional
    public AuthDto.MessageResponse updateCurrency(Long userId, String currency) {
        User user = getUserById(userId);
        user.setCurrency(currency);
        userRepository.save(user);
        return new AuthDto.MessageResponse("Currency updated to " + currency, true);
    }

    @Override
    @Transactional
    public AuthDto.MessageResponse updateMonthlyBudget(Long userId, Double budget) {
        User user = getUserById(userId);
        user.setMonthlyBudget(budget);
        userRepository.save(user);
        return new AuthDto.MessageResponse("Monthly budget goal updated", true);
    }

    @Override
    @Transactional
    public AuthDto.MessageResponse deactivateAccount(Long userId) {
        User user = getUserById(userId);
        user.setIsActive(false);
        userRepository.save(user);
        return new AuthDto.MessageResponse("Account deactivated. Your data is preserved.", true);
    }

    // ─── Forgot Password ──────────────────────────────────────
    @Override
    @Transactional
    public AuthDto.MessageResponse forgotPassword(AuthDto.ForgotPasswordRequest request) {
        // Always return success to avoid email enumeration attacks
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            if (!Boolean.TRUE.equals(user.getIsActive())) return;

            // Invalidate any previous tokens for this user
            resetTokenRepository.deleteAllByUser(user);

            String rawToken = String.format("%06d", SECURE_RANDOM.nextInt(1000000));
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(rawToken)
                    .user(user)
                    .expiresAt(LocalDateTime.now().plusMinutes(resetExpiryMinutes))
                    .used(false)
                    .build();
            resetTokenRepository.save(resetToken);

            emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), rawToken);
        });

        return new AuthDto.MessageResponse(
                "If that email is registered, you'll receive a reset link shortly.", true);
    }

    // ─── Reset Password ───────────────────────────────────────
    @Override
    @Transactional
    public AuthDto.MessageResponse resetPassword(AuthDto.ResetPasswordRequest request) {
        PasswordResetToken resetToken = resetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset link."));

        if (resetToken.isUsed()) {
            throw new IllegalArgumentException("This reset link has already been used.");
        }
        if (resetToken.isExpired()) {
            throw new IllegalArgumentException("This reset link has expired. Please request a new one.");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        resetTokenRepository.save(resetToken);

        return new AuthDto.MessageResponse("Password reset successfully. You can now sign in.", true);
    }

    @Override
    @Transactional
    public AuthDto.AuthResponse googleLogin(AuthDto.GoogleLoginRequest request) {
        try {
            // Use Google's tokeninfo endpoint — delegates ALL validation to Google's servers.
            // This avoids JWKS network calls, clock-skew issues, and key-rotation problems.
            java.net.http.HttpRequest httpRequest = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(
                            "https://oauth2.googleapis.com/tokeninfo?id_token=" + request.getIdToken()))
                    .GET()
                    .build();

            java.net.http.HttpResponse<String> response =
                    httpClient.send(httpRequest, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                // Google returned an error — token is invalid or expired
                throw new BadCredentialsException(
                        "Google token validation failed. Please sign in again.");
            }

            // Parse the tokeninfo JSON payload
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode payload = mapper.readTree(response.body());

            // Verify this token was issued for OUR app (prevents token substitution attacks).
            // For web-based Google Sign-In, the client ID can appear in either `aud` (audience)
            // or `azp` (authorized party / presenter) depending on the OAuth flow used.
            String aud = payload.path("aud").asText("");
            String azp = payload.path("azp").asText("");
            String clientId = googleClientId.trim();
            if (!clientId.equals(aud) && !clientId.equals(azp)) {
                throw new BadCredentialsException(
                        "Google token audience mismatch. Expected: " + clientId
                        + " | Got aud=" + aud + ", azp=" + azp);
            }

            // Verify email is confirmed by Google
            boolean emailVerified = payload.path("email_verified").asText("false").equals("true");
            if (!emailVerified) {
                throw new BadCredentialsException("Google account email is not verified.");
            }

            String email      = payload.path("email").asText();
            String name       = payload.path("name").asText(null);
            String pictureUrl = payload.path("picture").asText(null);

            if (email == null || email.isBlank()) {
                throw new BadCredentialsException("Could not retrieve email from Google account.");
            }

            User user = userRepository.findByEmail(email).orElse(null);

            if (user == null) {
                // First-time Google login — auto-register
                user = User.builder()
                        .fullName(name != null && !name.isBlank() ? name : "Google User")
                        .email(email)
                        .passwordHash("") // No password for OAuth users
                        .provider(User.AuthProvider.GOOGLE)
                        .isActive(true)
                        .role(User.Role.USER)
                        .avatarUrl(pictureUrl)
                        .currency("INR")
                        .timezone("Asia/Kolkata")
                        .build();
                user = userRepository.save(user);
            } else if (!Boolean.TRUE.equals(user.getIsActive())) {
                throw new IllegalStateException("Account is deactivated.");
            }
            // Existing LOCAL users can also log in via Google (email-based account linking)

            String jwtToken = jwtService.generateToken(user.getEmail(), user.getUserId(), user.getRole().name());
            
            // Send async login notification email
            emailService.sendLoginNotification(user.getEmail(), user.getFullName());
            
            return new AuthDto.AuthResponse(jwtToken, toUserResponse(user));

        } catch (BadCredentialsException | IllegalStateException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BadCredentialsException("Google authentication interrupted");
        } catch (Exception e) {
            throw new BadCredentialsException("Google authentication failed: " + e.getMessage());
        }
    }

    // ─── Mapper ───────────────────────────────────────────
    private AuthDto.UserResponse toUserResponse(User user) {
        AuthDto.UserResponse res = new AuthDto.UserResponse();
        res.setUserId(user.getUserId());
        res.setFullName(user.getFullName());
        res.setEmail(user.getEmail());
        res.setCurrency(user.getCurrency());
        res.setTimezone(user.getTimezone());
        res.setAvatarUrl(user.getAvatarUrl());
        res.setBio(user.getBio());
        res.setProvider(user.getProvider().name());
        res.setIsActive(user.getIsActive());
        res.setMonthlyBudget(user.getMonthlyBudget());
        res.setRole(user.getRole().name());
        res.setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
        return res;
    }
}
