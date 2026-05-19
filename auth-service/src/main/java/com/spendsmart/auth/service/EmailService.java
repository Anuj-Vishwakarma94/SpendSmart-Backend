package com.spendsmart.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    // ─── Login Notification ───────────────────────────────────────────────
    @Async
    public void sendLoginNotification(String toEmail, String fullName) {
        try {
            log.info("Attempting to send login notification to {}", toEmail);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "SpendSmart");
            helper.setTo(toEmail);
            helper.setSubject("New sign-in to your SpendSmart account");

            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));

            String html = "<!DOCTYPE html>" +
                    "<html><body style='margin:0;padding:0;font-family:Arial,sans-serif;background:#f4f4f4;'>" +
                    "<div style='max-width:600px;margin:32px auto;background:#ffffff;border-radius:10px;" +
                    "border:1px solid #e0e0e0;overflow:hidden;'>" +
                    "<div style='background:linear-gradient(135deg,#6c63ff,#4a90e2);padding:28px 32px;'>" +
                    "<h1 style='margin:0;color:#fff;font-size:22px;'>SpendSmart Security Alert</h1></div>" +
                    "<div style='padding:28px 32px;color:#333;'>" +
                    "<p style='font-size:16px;'>Hi <strong>" + fullName + "</strong>,</p>" +
                    "<p>We noticed a new sign-in to your <strong>SpendSmart</strong> account.</p>" +
                    "<table style='width:100%;background:#f8f8ff;border-radius:8px;padding:16px;border-collapse:collapse;'>" +
                    "<tr><td style='padding:6px 12px;color:#777;'>Time</td>" +
                    "<td style='padding:6px 12px;font-weight:bold;'>" + time + "</td></tr>" +
                    "</table>" +
                    "<p style='margin-top:20px;'>If this was <strong>you</strong>, no action is needed — you're all set!</p>" +
                    "<p style='color:#777;font-size:13px;'>If you did not sign in, please contact us immediately at " + fromEmail + ".</p>" +
                    "</div>" +
                    "<div style='padding:16px 32px;background:#f9f9f9;border-top:1px solid #eee;" +
                    "font-size:12px;color:#999;text-align:center;'>" +
                    "You received this email because a sign-in was detected on your SpendSmart account.<br/>" +
                    "&copy; 2025 SpendSmart. All rights reserved.</div>" +
                    "</div></body></html>";

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Login notification sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send login notification to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    // ─── Password Reset Email ─────────────────────────────────────────────
    public void sendPasswordResetEmail(String toEmail, String fullName, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "SpendSmart");
            helper.setTo(toEmail);
            helper.setSubject("Reset your SpendSmart password");

            String html = "<!DOCTYPE html>" +
                    "<html><body style='margin:0;padding:0;font-family:Arial,sans-serif;background:#f4f4f4;'>" +
                    "<div style='max-width:600px;margin:32px auto;background:#ffffff;border-radius:10px;" +
                    "border:1px solid #e0e0e0;overflow:hidden;'>" +
                    "<div style='background:linear-gradient(135deg,#6c63ff,#4a90e2);padding:28px 32px;'>" +
                    "<h1 style='margin:0;color:#fff;font-size:22px;'>Password Reset Request</h1></div>" +
                    "<div style='padding:28px 32px;color:#333;'>" +
                    "<p style='font-size:16px;'>Hi <strong>" + fullName + "</strong>,</p>" +
                    "<p>We received a request to reset your SpendSmart password.</p>" +
                    "<p>Use the OTP below to reset your password (expires in <strong>30 minutes</strong>):</p>" +
                    "<div style='text-align:center;margin:28px 0;'>" +
                    "<span style='font-size:36px;font-weight:bold;letter-spacing:12px;color:#6c63ff;" +
                    "background:#f0eeff;padding:16px 28px;border-radius:8px;display:inline-block;'>" +
                    token + "</span></div>" +
                    "<p>Enter this code on the reset password page to choose a new password.</p>" +
                    "<p style='color:#999;font-size:13px;'>If you didn't request a password reset, you can safely ignore this email.</p>" +
                    "</div>" +
                    "<div style='padding:16px 32px;background:#f9f9f9;border-top:1px solid #eee;" +
                    "font-size:12px;color:#999;text-align:center;'>" +
                    "&copy; 2025 SpendSmart. All rights reserved.</div>" +
                    "</div></body></html>";

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("SMTP ERROR sending password reset to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }
}
