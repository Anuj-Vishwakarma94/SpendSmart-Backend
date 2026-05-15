package com.spendsmart.subscription.config;

import com.spendsmart.subscription.dto.SubscriptionDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Feign client to call payment-service via Eureka service discovery.
 * subscription-service delegates order creation & verification to payment-service.
 */
@FeignClient(name = "payment-service")
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
}
