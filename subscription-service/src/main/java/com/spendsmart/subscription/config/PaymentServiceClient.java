package com.spendsmart.subscription.config;

import com.spendsmart.subscription.dto.SubscriptionDto;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Feign client to call payment-service via Eureka service discovery.
 * subscription-service delegates order creation & verification to payment-service.
 */
@FeignClient(name = "payment-service", configuration = PaymentServiceClient.FeignConfig.class)
public interface PaymentServiceClient {

    @PostMapping("/api/payments/create-order")
    SubscriptionDto.PaymentOrderResponse createOrder(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody SubscriptionDto.PaymentOrderRequest request
    );

    @PostMapping("/api/payments/verify")
    Object verifyPayment(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody SubscriptionDto.PaymentVerifyRequest request
    );

    @Slf4j
    class FeignConfig {
        @Bean
        public ErrorDecoder paymentServiceErrorDecoder() {
            return (methodKey, response) -> {
                String body = "";
                try {
                    if (response.body() != null) {
                        body = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    }
                } catch (IOException ignored) {}
                log.error("payment-service error — status={} method={} body={}", 
                    response.status(), methodKey, body);
                return new RuntimeException(
                    "Payment service error (HTTP " + response.status() + "): " + body);
            };
        }
    }
}

