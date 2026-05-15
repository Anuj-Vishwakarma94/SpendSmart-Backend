package com.spendsmart.auth.resource;

import com.spendsmart.auth.dto.AuthDto;
import com.spendsmart.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthResourceTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthResource authResource;

    private AuthDto.AuthResponse authResponse;
    private AuthDto.MessageResponse messageResponse;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        AuthDto.UserResponse userResponse = new AuthDto.UserResponse();
        userResponse.setEmail("test@example.com");
        authResponse = new AuthDto.AuthResponse("dummy-token", userResponse);
        messageResponse = new AuthDto.MessageResponse("Success", true);
        authentication = mock(Authentication.class);
    }

    @Test
    void register_ReturnsCreated() {
        AuthDto.RegisterRequest request = new AuthDto.RegisterRequest();
        when(authService.register(any(AuthDto.RegisterRequest.class))).thenReturn(authResponse);

        ResponseEntity<AuthDto.AuthResponse> response = authResource.register(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(authResponse, response.getBody());
    }

    @Test
    void login_ReturnsOk() {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest();
        when(authService.login(any(AuthDto.LoginRequest.class))).thenReturn(authResponse);

        ResponseEntity<AuthDto.AuthResponse> response = authResource.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(authResponse, response.getBody());
    }

    @Test
    void googleLogin_ReturnsOk() {
        AuthDto.GoogleLoginRequest request = new AuthDto.GoogleLoginRequest();
        when(authService.googleLogin(any(AuthDto.GoogleLoginRequest.class))).thenReturn(authResponse);

        ResponseEntity<AuthDto.AuthResponse> response = authResource.googleLogin(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(authResponse, response.getBody());
    }

    @Test
    void logout_ReturnsOk() {
        when(authService.logout("valid-token")).thenReturn(messageResponse);
        ResponseEntity<AuthDto.MessageResponse> response = authResource.logout("Bearer valid-token");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(messageResponse, response.getBody());
    }

    @Test
    void validate_ValidToken_ReturnsOk() {
        when(authService.validateToken("valid-token")).thenReturn(true);
        ResponseEntity<Map<String, Boolean>> response = authResource.validateToken("Bearer valid-token");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(true, response.getBody().get("valid"));
    }

    @Test
    void validate_InvalidToken_ReturnsOkWithFalse() {
        when(authService.validateToken("invalid-token")).thenReturn(false);
        ResponseEntity<Map<String, Boolean>> response = authResource.validateToken("Bearer invalid-token");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(false, response.getBody().get("valid"));
    }

    @Test
    void refreshToken_ReturnsOk() {
        when(authService.refreshToken("valid-token")).thenReturn("new-token");
        ResponseEntity<Map<String, String>> response = authResource.refreshToken("Bearer valid-token");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("new-token", response.getBody().get("token"));
    }

    @Test
    void getProfile_ReturnsOk() {
        when(authentication.getDetails()).thenReturn(1L);
        AuthDto.UserResponse userResponse = new AuthDto.UserResponse();
        when(authService.updateProfile(eq(1L), any(AuthDto.UpdateProfileRequest.class))).thenReturn(userResponse);

        ResponseEntity<AuthDto.UserResponse> response = authResource.getProfile(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(userResponse, response.getBody());
    }

    @Test
    void updateProfile_ReturnsOk() {
        when(authentication.getDetails()).thenReturn(1L);
        AuthDto.UpdateProfileRequest request = new AuthDto.UpdateProfileRequest();
        AuthDto.UserResponse userResponse = new AuthDto.UserResponse();
        when(authService.updateProfile(1L, request)).thenReturn(userResponse);

        ResponseEntity<AuthDto.UserResponse> response = authResource.updateProfile(authentication, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(userResponse, response.getBody());
    }

    @Test
    void changePassword_ReturnsOk() {
        when(authentication.getDetails()).thenReturn(1L);
        AuthDto.ChangePasswordRequest request = new AuthDto.ChangePasswordRequest();
        when(authService.changePassword(1L, request)).thenReturn(messageResponse);

        ResponseEntity<AuthDto.MessageResponse> response = authResource.changePassword(authentication, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(messageResponse, response.getBody());
    }

    @Test
    void updateCurrency_ReturnsOk() {
        when(authentication.getDetails()).thenReturn(1L);
        AuthDto.UpdateCurrencyRequest request = new AuthDto.UpdateCurrencyRequest();
        request.setCurrency("EUR");
        when(authService.updateCurrency(1L, "EUR")).thenReturn(messageResponse);

        ResponseEntity<AuthDto.MessageResponse> response = authResource.updateCurrency(authentication, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(messageResponse, response.getBody());
    }

    @Test
    void updateBudget_ReturnsOk() {
        when(authentication.getDetails()).thenReturn(1L);
        AuthDto.UpdateBudgetRequest request = new AuthDto.UpdateBudgetRequest();
        request.setMonthlyBudget(5000.0);
        when(authService.updateMonthlyBudget(1L, 5000.0)).thenReturn(messageResponse);

        ResponseEntity<AuthDto.MessageResponse> response = authResource.updateBudget(authentication, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(messageResponse, response.getBody());
    }

    @Test
    void deactivateAccount_ReturnsOk() {
        when(authentication.getDetails()).thenReturn(1L);
        when(authService.deactivateAccount(1L)).thenReturn(messageResponse);

        ResponseEntity<AuthDto.MessageResponse> response = authResource.deactivateAccount(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(messageResponse, response.getBody());
    }

    @Test
    void forgotPassword_ReturnsOk() {
        AuthDto.ForgotPasswordRequest request = new AuthDto.ForgotPasswordRequest();
        when(authService.forgotPassword(request)).thenReturn(messageResponse);

        ResponseEntity<AuthDto.MessageResponse> response = authResource.forgotPassword(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(messageResponse, response.getBody());
    }

    @Test
    void resetPassword_ReturnsOk() {
        AuthDto.ResetPasswordRequest request = new AuthDto.ResetPasswordRequest();
        when(authService.resetPassword(request)).thenReturn(messageResponse);

        ResponseEntity<AuthDto.MessageResponse> response = authResource.resetPassword(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(messageResponse, response.getBody());
    }
}
