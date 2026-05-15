package com.spendsmart.auth.service;

import com.spendsmart.auth.dto.AuthDto;
import com.spendsmart.auth.entity.User;

public interface AuthService {

    AuthDto.AuthResponse register(AuthDto.RegisterRequest request);

    AuthDto.AuthResponse login(AuthDto.LoginRequest request);

    AuthDto.AuthResponse googleLogin(AuthDto.GoogleLoginRequest request);

    AuthDto.MessageResponse logout(String token);

    boolean validateToken(String token);

    String refreshToken(String token);

    User getUserById(Long userId);

    User getUserByEmail(String email);

    AuthDto.UserResponse updateProfile(Long userId, AuthDto.UpdateProfileRequest request);

    AuthDto.MessageResponse changePassword(Long userId, AuthDto.ChangePasswordRequest request);

    AuthDto.MessageResponse updateCurrency(Long userId, String currency);

    AuthDto.MessageResponse updateMonthlyBudget(Long userId, Double budget);

    AuthDto.MessageResponse deactivateAccount(Long userId);

    AuthDto.MessageResponse forgotPassword(AuthDto.ForgotPasswordRequest request);

    AuthDto.MessageResponse resetPassword(AuthDto.ResetPasswordRequest request);
}
