package com.spendsmart.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // User who initiated the payment
    private Long userId;

    // Razorpay Order ID (e.g., order_xxxxxxxxxx)
    @Column(unique = true)
    private String razorpayOrderId;

    // Razorpay Payment ID after successful capture (e.g., pay_xxxxxxxxxx)
    private String razorpayPaymentId;

    // Razorpay Signature for verification
    private String razorpaySignature;

    // Amount in smallest currency unit (paise for INR)
    private Long amountInPaise;

    // Readable amount (e.g., 500.00)
    private Double amount;

    @Builder.Default
    private String currency = "INR";

    private String description;

    // Linked expense (optional — if payment is for an expense)
    private Long expenseId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.CREATED;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentMethod method = PaymentMethod.UNKNOWN;

    // Refund tracking
    private String razorpayRefundId;

    @Enumerated(EnumType.STRING)
    private RefundStatus refundStatus;

    private Long refundAmountInPaise;

    // Receipt / notes
    private String receiptId;
    private String notes;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ─── Enums ───────────────────────────────────────────────

    public enum PaymentStatus {
        CREATED,      // Order created, awaiting payment
        ATTEMPTED,    // Payment attempted but not captured
        PAID,         // Payment successfully captured
        FAILED,       // Payment failed
        REFUNDED,     // Fully refunded
        PARTIALLY_REFUNDED
    }

    public enum PaymentMethod {
        UPI, CARD, NET_BANKING, WALLET, EMI, UNKNOWN
    }

    public enum RefundStatus {
        PENDING, PROCESSED, FAILED
    }
}
