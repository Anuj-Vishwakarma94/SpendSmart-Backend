package com.spendsmart.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;

public class PaymentDto {

    // ─── Request DTOs ─────────────────────────────────────────

    /** Step 1: Client requests a Razorpay order */
    @Data
    public static class CreateOrderRequest {
        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        private Double amount;           // In rupees (e.g., 500.00)

        private String currency = "INR";
        private String description;
        private Long expenseId;          // Optional: link to an expense
        private String notes;
    }

    /** Step 2: Client sends payment result for verification */
    @Data
    public static class VerifyPaymentRequest {
        @NotNull
        private String razorpayOrderId;

        @NotNull
        private String razorpayPaymentId;

        @NotNull
        private String razorpaySignature;
    }

    /** Refund request */
    @Data
    public static class RefundRequest {
        @NotNull
        private String razorpayPaymentId;

        private Double refundAmount;     // null = full refund
        private String reason;
    }

    // ─── Response DTOs ────────────────────────────────────────

    /** Returned to client after creating Razorpay order */
    @Data
    public static class OrderResponse {
        private String razorpayOrderId;
        private Double amount;
        private String currency;
        private String status;
        private String keyId;            // Public key sent to frontend
        private String description;
        private Long paymentDbId;        // Internal DB record ID
    }

    /** Returned after payment verification */
    @Data
    public static class PaymentResponse {
        private Long id;
        private Long userId;
        private String razorpayOrderId;
        private String razorpayPaymentId;
        private Double amount;
        private String currency;
        private String status;
        private String method;
        private String description;
        private Long expenseId;
        private String refundStatus;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    /** Refund response */
    @Data
    public static class RefundResponse {
        private String razorpayRefundId;
        private String razorpayPaymentId;
        private Double refundAmount;
        private String status;
        private String message;
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
}
