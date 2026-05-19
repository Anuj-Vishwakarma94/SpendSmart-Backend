package com.spendsmart.payment.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RazorpayConfig {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Bean
    public RazorpayClient razorpayClient() {
        try {
            log.info("Initializing Razorpay client with keyId: {}***", 
                keyId != null && keyId.length() > 8 ? keyId.substring(0, 8) : keyId);
            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            log.info("Razorpay client initialized successfully");
            return client;
        } catch (RazorpayException e) {
            log.error("FATAL: Failed to initialize Razorpay client. " +
                "Check RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET env vars. Error: {}", e.getMessage());
            // Re-throw as unchecked so Spring still fails fast with a clear message
            throw new IllegalStateException(
                "Failed to initialize Razorpay client — check RAZORPAY_KEY_ID / RAZORPAY_KEY_SECRET: " 
                + e.getMessage(), e);
        }
    }
}
