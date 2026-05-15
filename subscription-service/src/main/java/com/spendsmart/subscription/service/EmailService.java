package com.spendsmart.subscription.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Async
    public void sendPremiumWelcomeEmail(String toEmail, String amountPaid) {
        try {
            log.info("Sending premium welcome email to: {}", toEmail);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "SpendSmart");
            helper.setTo(toEmail);
            helper.setSubject("Welcome to SpendSmart Premium! 🎉");

            String purchaseDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
            String expiryDate   = LocalDate.now().plusDays(30).format(DateTimeFormatter.ofPattern("dd MMM yyyy"));

            String html = "<!DOCTYPE html>" +
                "<html lang='en'><head><meta charset='UTF-8'/></head>" +
                "<body style='margin:0;padding:0;background:#0f0f1a;font-family:Arial,sans-serif;'>" +

                // ── Outer wrapper ──
                "<table width='100%' cellpadding='0' cellspacing='0' border='0' style='background:#0f0f1a;padding:32px 16px;'>" +
                "<tr><td align='center'>" +
                "<table width='600' cellpadding='0' cellspacing='0' border='0' style='max-width:600px;width:100%;border-radius:16px;overflow:hidden;border:1px solid rgba(108,99,255,0.3);'>" +

                // ── Header ──
                "<tr><td style='background:linear-gradient(135deg,#6c63ff 0%,#a855f7 50%,#ec4899 100%);padding:40px 36px;text-align:center;'>" +
                "<div style='font-size:48px;margin-bottom:12px;'>👑</div>" +
                "<h1 style='margin:0;color:#ffffff;font-size:28px;font-weight:700;letter-spacing:-0.5px;'>Welcome to Premium</h1>" +
                "<p style='margin:10px 0 0;color:rgba(255,255,255,0.85);font-size:15px;'>Welcome to the SpendSmart Premium family</p>" +
                "</td></tr>" +

                // ── Body ──
                "<tr><td style='background:#1a1a2e;padding:36px;'>" +

                // Greeting
                "<p style='color:#e0e0ff;font-size:16px;margin:0 0 8px;'>Hi there,</p>" +
                "<p style='color:#a0a0c0;font-size:15px;line-height:1.7;margin:0 0 28px;'>" +
                "Thank you for upgrading to <strong style='color:#a855f7;'>SpendSmart Premium</strong>! " +
                "Your payment has been confirmed and your account is now fully unlocked." +
                "</p>" +

                // Receipt card
                "<div style='background:#0f0f1a;border:1px solid rgba(108,99,255,0.25);border-radius:12px;padding:24px;margin-bottom:28px;'>" +
                "<p style='margin:0 0 16px;color:#6c63ff;font-size:13px;font-weight:700;text-transform:uppercase;letter-spacing:1px;'>Payment Receipt</p>" +
                "<table width='100%' cellpadding='0' cellspacing='0' border='0'>" +
                "<tr>" +
                "<td style='color:#888;font-size:14px;padding:8px 0;border-bottom:1px solid rgba(255,255,255,0.06);'>Amount Paid</td>" +
                "<td style='color:#fff;font-size:14px;font-weight:700;text-align:right;padding:8px 0;border-bottom:1px solid rgba(255,255,255,0.06);'>" + amountPaid + "</td>" +
                "</tr>" +
                "<tr>" +
                "<td style='color:#888;font-size:14px;padding:8px 0;border-bottom:1px solid rgba(255,255,255,0.06);'>Plan</td>" +
                "<td style='color:#fff;font-size:14px;font-weight:700;text-align:right;padding:8px 0;border-bottom:1px solid rgba(255,255,255,0.06);'>Premium Monthly</td>" +
                "</tr>" +
                "<tr>" +
                "<td style='color:#888;font-size:14px;padding:8px 0;border-bottom:1px solid rgba(255,255,255,0.06);'>Start Date</td>" +
                "<td style='color:#fff;font-size:14px;text-align:right;padding:8px 0;border-bottom:1px solid rgba(255,255,255,0.06);'>" + purchaseDate + "</td>" +
                "</tr>" +
                "<tr>" +
                "<td style='color:#888;font-size:14px;padding:8px 0;'>Valid Until</td>" +
                "<td style='color:#a855f7;font-size:14px;font-weight:700;text-align:right;padding:8px 0;'>" + expiryDate + "</td>" +
                "</tr>" +
                "</table></div>" +

                // Features unlocked
                "<p style='color:#6c63ff;font-size:13px;font-weight:700;text-transform:uppercase;letter-spacing:1px;margin:0 0 14px;'>Features Unlocked</p>" +
                "<table width='100%' cellpadding='0' cellspacing='0' border='0' style='margin-bottom:28px;'>" +
                featureRow("📊", "Advanced Analytics", "Deep insights into your spending patterns & trends") +
                featureRow("🔄", "Recurring Payments", "Automate your bills and subscriptions effortlessly") +
                featureRow("💰", "Unlimited Budgets", "Create as many budgets as you need, no limits") +
                featureRow("🎯", "Priority Support", "Get help faster with dedicated premium support") +
                "</table>" +



                "</td></tr>" +

                // ── Footer ──
                "<tr><td style='background:#0f0f1a;padding:24px 36px;text-align:center;border-top:1px solid rgba(255,255,255,0.06);'>" +
                "<p style='color:#555;font-size:12px;margin:0 0 4px;'>You're receiving this because you just upgraded to SpendSmart Premium.</p>" +
                "<p style='color:#444;font-size:11px;margin:0;'>© 2025 SpendSmart. All rights reserved.</p>" +
                "</td></tr>" +

                "</table>" +
                "</td></tr></table>" +
                "</body></html>";

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Premium welcome email successfully sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send premium welcome email to: {}", toEmail, e);
        }
    }

    // ── Helper: Feature row ──────────────────────────────────────────────
    private static String featureRow(String icon, String title, String desc) {
        return "<tr>" +
               "<td style='padding:10px 0;vertical-align:top;width:52px;'>" +
               "<table cellpadding='0' cellspacing='0' border='0' width='44' height='44'>" +
               "<tr><td width='44' height='44' align='center' valign='middle' " +
               "style='width:44px;height:44px;background:rgba(108,99,255,0.15);border-radius:10px;" +
               "font-size:22px;text-align:center;vertical-align:middle;line-height:44px;'>" +
               icon +
               "</td></tr></table>" +
               "</td>" +
               "<td style='padding:10px 0 10px 14px;vertical-align:middle;'>" +
               "<p style='margin:0 0 3px;color:#e0e0ff;font-size:14px;font-weight:700;'>" + title + "</p>" +
               "<p style='margin:0;color:#888;font-size:13px;'>" + desc + "</p>" +
               "</td></tr>";
    }
}
