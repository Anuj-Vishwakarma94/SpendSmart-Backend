package com.spendsmart.subscription.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PlanType planType = PlanType.FREE;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.FREE;

    // Dates for PREMIUM subscriptions
    private LocalDate startDate;
    private LocalDate endDate;          // null for FREE

    // Payment linkage
    private String razorpayOrderId;
    private String razorpayPaymentId;

    @Builder.Default
    private Double amountPaid = 0.0;

    @Builder.Default
    private String currency = "INR";

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ─── Helpers ─────────────────────────────────────────────

    public boolean isPremiumActive() {
        return status == SubscriptionStatus.ACTIVE
                && endDate != null
                && !LocalDate.now().isAfter(endDate);
    }

    // ─── Enums ───────────────────────────────────────────────

    public enum PlanType {
        FREE, PREMIUM_MONTHLY
    }

    public enum SubscriptionStatus {
        FREE,       // Never subscribed
        PENDING,    // Order created, payment not yet done
        ACTIVE,     // Paid and within validity
        EXPIRED,    // Validity passed
        CANCELLED
    }
}
