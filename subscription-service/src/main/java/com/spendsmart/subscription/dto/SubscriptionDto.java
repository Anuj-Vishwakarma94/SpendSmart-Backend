package com.spendsmart.subscription.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class SubscriptionDto {

    /** Returned when user checks their current plan */
    @Data
    public static class SubscriptionStatusResponse {
        private Long userId;
        private String planType;
        private String status;
        private boolean isPremium;
        private LocalDate startDate;
        private LocalDate endDate;
        private Long daysRemaining;
        private Double amountPaid;
        private String currency;
        private LocalDateTime updatedAt;
    }

    /** Returned when a new checkout is initiated */
    @Data
    public static class CheckoutResponse {
        private String razorpayOrderId;
        private Double amount;
        private String currency;
        private String keyId;           // Razorpay public key — needed by frontend checkout
        private Long subscriptionDbId;
        private String planType;
        private String description;
    }

    /** Frontend sends this after Razorpay payment succeeds */
    @Data
    public static class ActivateRequest {
        private String razorpayOrderId;
        private String razorpayPaymentId;
        private String razorpaySignature;
    }

    @Data
    public static class MessageResponse {
        private String message;
        private boolean success;

        public MessageResponse(String message, boolean success) {
            this.message = message;
            this.success = success;
        }
    }

    /** Internal DTO sent to payment-service */
    @Data
    public static class PaymentOrderRequest {
        private Double amount;
        private String currency;
        private String description;
        private String notes;
    }

    /** Partial mapping of payment-service's OrderResponse */
    @Data
    public static class PaymentOrderResponse {
        private String razorpayOrderId;
        private Double amount;
        private String currency;
        private String keyId;
        private Long paymentDbId;
    }

    /** Partial mapping of payment-service's VerifyPaymentRequest */
    @Data
    public static class PaymentVerifyRequest {
        private String razorpayOrderId;
        private String razorpayPaymentId;
        private String razorpaySignature;
    }
}
