package com.spendsmart.auth.serviceimpl;

import com.spendsmart.auth.config.JwtService;
import com.spendsmart.auth.dto.AuthDto;
import com.spendsmart.auth.entity.PasswordResetToken;
import com.spendsmart.auth.entity.User;
import com.spendsmart.auth.repository.PasswordResetTokenRepository;
import com.spendsmart.auth.repository.UserRepository;
import com.spendsmart.auth.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordResetTokenRepository resetTokenRepository;

    @Mock
    private java.net.http.HttpClient httpClient;

    @Mock
    private java.net.http.HttpResponse<String> httpResponse;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(1L)
                .fullName("Test User")
                .email("test@example.com")
                .passwordHash("hashedpassword")
                .currency("USD")
                .timezone("UTC")
                .provider(User.AuthProvider.LOCAL)
                .isActive(true)
                .role(User.Role.USER)
                .build();
                
        ReflectionTestUtils.setField(authService, "resetExpiryMinutes", 30);
        ReflectionTestUtils.setField(authService, "googleClientId", "dummy-google-client-id");
        ReflectionTestUtils.setField(authService, "httpClient", httpClient);
    }

    @Test
    void register_Success() {
        AuthDto.RegisterRequest request = new AuthDto.RegisterRequest();
        request.setFullName("Test User");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setCurrency("USD");
        request.setTimezone("UTC");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedpassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateToken(anyString(), anyLong(), anyString())).thenReturn("dummy-jwt-token");

        AuthDto.AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("dummy-jwt-token", response.getToken());
        assertEquals("test@example.com", response.getUser().getEmail());
        verify(emailService, times(1)).sendLoginNotification(anyString(), anyString());
    }

    @Test
    void register_EmailAlreadyExists_ThrowsException() {
        AuthDto.RegisterRequest request = new AuthDto.RegisterRequest();
        request.setEmail("test@example.com");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
    }

    @Test
    void login_Success() {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "hashedpassword")).thenReturn(true);
        when(jwtService.generateToken(anyString(), anyLong(), anyString())).thenReturn("dummy-jwt-token");

        AuthDto.AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("dummy-jwt-token", response.getToken());
        verify(emailService, times(1)).sendLoginNotification(anyString(), anyString());
    }

    @Test
    void login_InvalidPassword_ThrowsException() {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongpassword");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", "hashedpassword")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }
    
    @Test
    void logout_Success() {
        AuthDto.MessageResponse response = authService.logout("some-token");
        assertTrue(response.isSuccess());
        assertEquals("Logged out successfully", response.getMessage());
    }

    @Test
    void validateToken_ValidToken() {
        when(jwtService.isTokenValid("valid-token")).thenReturn(true);
        when(jwtService.isTokenExpired("valid-token")).thenReturn(false);

        assertTrue(authService.validateToken("valid-token"));
    }

    @Test
    void refreshToken_Success() {
        when(jwtService.isTokenValid("valid-token")).thenReturn(true);
        when(jwtService.extractEmail("valid-token")).thenReturn("test@example.com");
        when(jwtService.extractUserId("valid-token")).thenReturn(1L);
        when(jwtService.extractRole("valid-token")).thenReturn("USER");
        when(jwtService.generateToken(anyString(), anyLong(), anyString())).thenReturn("new-token");

        String newToken = authService.refreshToken("valid-token");
        assertEquals("new-token", newToken);
    }

    @Test
    void forgotPassword_SendsEmail() {
        AuthDto.ForgotPasswordRequest request = new AuthDto.ForgotPasswordRequest();
        request.setEmail("test@example.com");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        AuthDto.MessageResponse response = authService.forgotPassword(request);

        assertTrue(response.isSuccess());
        verify(resetTokenRepository, times(1)).deleteAllByUser(testUser);
        verify(resetTokenRepository, times(1)).save(any(PasswordResetToken.class));
        verify(emailService, times(1)).sendPasswordResetEmail(anyString(), anyString(), anyString());
    }

    @Test
    void resetPassword_Success() {
        AuthDto.ResetPasswordRequest request = new AuthDto.ResetPasswordRequest();
        request.setToken("123456");
        request.setNewPassword("newpassword");

        PasswordResetToken token = new PasswordResetToken();
        token.setToken("123456");
        token.setUsed(false);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        token.setUser(testUser);

        when(resetTokenRepository.findByToken("123456")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newpassword")).thenReturn("newhashedpassword");

        AuthDto.MessageResponse response = authService.resetPassword(request);

        assertTrue(response.isSuccess());
        assertTrue(token.isUsed());
        verify(userRepository, times(1)).save(testUser);
        verify(resetTokenRepository, times(1)).save(token);
    }

    @Test
    void getUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        User user = authService.getUserById(1L);
        assertEquals(testUser, user);
    }

    @Test
    void getUserByEmail_Success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        User user = authService.getUserByEmail("test@example.com");
        assertEquals(testUser, user);
    }

    @Test
    void updateProfile_Success() {
        AuthDto.UpdateProfileRequest request = new AuthDto.UpdateProfileRequest();
        request.setFullName("Updated Name");
        request.setBio("New Bio");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        AuthDto.UserResponse response = authService.updateProfile(1L, request);
        assertEquals("Updated Name", testUser.getFullName());
        assertEquals("New Bio", testUser.getBio());
        assertEquals(testUser.getEmail(), response.getEmail());
    }

    @Test
    void changePassword_Success() {
        AuthDto.ChangePasswordRequest request = new AuthDto.ChangePasswordRequest();
        request.setCurrentPassword("hashedpassword");
        request.setNewPassword("newpassword");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("hashedpassword", "hashedpassword")).thenReturn(true);
        when(passwordEncoder.encode("newpassword")).thenReturn("newhashedpassword");

        AuthDto.MessageResponse response = authService.changePassword(1L, request);
        assertTrue(response.isSuccess());
        verify(userRepository, times(1)).save(testUser);
        assertEquals("newhashedpassword", testUser.getPasswordHash());
    }

    @Test
    void updateCurrency_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        AuthDto.MessageResponse response = authService.updateCurrency(1L, "EUR");
        assertTrue(response.isSuccess());
        assertEquals("EUR", testUser.getCurrency());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void updateMonthlyBudget_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        AuthDto.MessageResponse response = authService.updateMonthlyBudget(1L, 5000.0);
        assertTrue(response.isSuccess());
        assertEquals(5000.0, testUser.getMonthlyBudget());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void deactivateAccount_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        AuthDto.MessageResponse response = authService.deactivateAccount(1L);
        assertTrue(response.isSuccess());
        assertFalse(testUser.getIsActive());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void googleLogin_Success_NewUser() throws Exception {
        AuthDto.GoogleLoginRequest request = new AuthDto.GoogleLoginRequest();
        request.setIdToken("dummy-id-token");

        String jsonPayload = """
                {
                    "aud": "dummy-google-client-id",
                    "email_verified": "true",
                    "email": "newuser@example.com",
                    "name": "New User",
                    "picture": "http://example.com/pic.jpg"
                }
                """;

        when(httpClient.<String>send(any(java.net.http.HttpRequest.class), any())).thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonPayload);

        when(userRepository.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(anyString(), any(), anyString())).thenReturn("google-jwt-token");

        AuthDto.AuthResponse response = authService.googleLogin(request);
        assertEquals("google-jwt-token", response.getToken());
        assertEquals("newuser@example.com", response.getUser().getEmail());
    }

    @Test
    void googleLogin_Success_ExistingUser() throws Exception {
        AuthDto.GoogleLoginRequest request = new AuthDto.GoogleLoginRequest();
        request.setIdToken("dummy-id-token");

        String jsonPayload = """
                {
                    "aud": "dummy-google-client-id",
                    "email_verified": "true",
                    "email": "test@example.com",
                    "name": "Test User",
                    "picture": "http://example.com/pic.jpg"
                }
                """;

        when(httpClient.<String>send(any(java.net.http.HttpRequest.class), any())).thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonPayload);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(anyString(), any(), anyString())).thenReturn("google-jwt-token");

        AuthDto.AuthResponse response = authService.googleLogin(request);
        assertEquals("google-jwt-token", response.getToken());
        assertEquals("test@example.com", response.getUser().getEmail());
    }

    @Test
    void googleLogin_InvalidAudience_ThrowsException() throws Exception {
        AuthDto.GoogleLoginRequest request = new AuthDto.GoogleLoginRequest();
        request.setIdToken("dummy-id-token");

        String jsonPayload = """
                {
                    "aud": "wrong-client-id",
                    "email_verified": "true",
                    "email": "test@example.com"
                }
                """;

        when(httpClient.<String>send(any(java.net.http.HttpRequest.class), any())).thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonPayload);

        assertThrows(BadCredentialsException.class, () -> authService.googleLogin(request));
    }

    @Test
    void googleLogin_HttpError_ThrowsException() throws Exception {
        AuthDto.GoogleLoginRequest request = new AuthDto.GoogleLoginRequest();
        request.setIdToken("dummy-id-token");

        when(httpClient.<String>send(any(java.net.http.HttpRequest.class), any())).thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(400);

        assertThrows(BadCredentialsException.class, () -> authService.googleLogin(request));
    }
}
