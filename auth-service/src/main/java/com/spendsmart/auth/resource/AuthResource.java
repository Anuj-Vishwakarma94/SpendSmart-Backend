package com.spendsmart.auth.resource;

import com.spendsmart.auth.dto.AuthDto;
import com.spendsmart.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthResource {

    private final AuthService authService;

    // POST /api/auth/register
    @PostMapping("/register")
    public ResponseEntity<AuthDto.AuthResponse> register(@Valid @RequestBody AuthDto.RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    // POST /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<AuthDto.AuthResponse> login(@Valid @RequestBody AuthDto.LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // POST /api/auth/google
    @PostMapping("/google")
    public ResponseEntity<AuthDto.AuthResponse> googleLogin(@Valid @RequestBody AuthDto.GoogleLoginRequest request) {
        return ResponseEntity.ok(authService.googleLogin(request));
    }

    // POST /api/auth/logout
    @PostMapping("/logout")
    public ResponseEntity<AuthDto.MessageResponse> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return ResponseEntity.ok(authService.logout(token));
    }

    // GET /api/auth/validate
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Boolean>> validateToken(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        boolean valid = authService.validateToken(token);
        return ResponseEntity.ok(Map.of("valid", valid));
    }

    // POST /api/auth/refresh
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshToken(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String newToken = authService.refreshToken(token);
        return ResponseEntity.ok(Map.of("token", newToken, "tokenType", "Bearer"));
    }

    // GET /api/auth/profile
    @GetMapping("/profile")
    public ResponseEntity<AuthDto.UserResponse> getProfile(Authentication auth) {
        Long userId = (Long) auth.getDetails();
        var user = authService.getUserById(userId);
        // Re-use update with empty to just fetch
        return ResponseEntity.ok(authService.updateProfile(userId, new AuthDto.UpdateProfileRequest()));
    }

    // PUT /api/auth/profile
    @PutMapping("/profile")
    public ResponseEntity<AuthDto.UserResponse> updateProfile(
            Authentication auth,
            @RequestBody AuthDto.UpdateProfileRequest request) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(authService.updateProfile(userId, request));
    }

    // PUT /api/auth/password
    @PutMapping("/password")
    public ResponseEntity<AuthDto.MessageResponse> changePassword(
            Authentication auth,
            @Valid @RequestBody AuthDto.ChangePasswordRequest request) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(authService.changePassword(userId, request));
    }

    // PUT /api/auth/currency
    @PutMapping("/currency")
    public ResponseEntity<AuthDto.MessageResponse> updateCurrency(
            Authentication auth,
            @RequestBody AuthDto.UpdateCurrencyRequest request) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(authService.updateCurrency(userId, request.getCurrency()));
    }

    // PUT /api/auth/budget
    @PutMapping("/budget")
    public ResponseEntity<AuthDto.MessageResponse> updateBudget(
            Authentication auth,
            @RequestBody AuthDto.UpdateBudgetRequest request) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(authService.updateMonthlyBudget(userId, request.getMonthlyBudget()));
    }

    // DELETE /api/auth/deactivate
    @DeleteMapping("/deactivate")
    public ResponseEntity<AuthDto.MessageResponse> deactivateAccount(Authentication auth) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(authService.deactivateAccount(userId));
    }

    // POST /api/auth/forgot-password  (public)
    @PostMapping("/forgot-password")
    public ResponseEntity<AuthDto.MessageResponse> forgotPassword(
            @Valid @RequestBody AuthDto.ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    // POST /api/auth/reset-password  (public)
    @PostMapping("/reset-password")
    public ResponseEntity<AuthDto.MessageResponse> resetPassword(
            @Valid @RequestBody AuthDto.ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }
}

