package com.spendsmart.recurring.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "recurring_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RecurringTransaction {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recurringId;

    @NotNull private Long userId;
    private Long categoryId;

    @NotBlank private String title;

    @Positive @NotNull private Double amount;

    @Enumerated(EnumType.STRING)
    @NotNull private TransactionType type;   // EXPENSE | INCOME

    @Enumerated(EnumType.STRING)
    @NotNull private Frequency frequency;

    @NotNull private LocalDate startDate;
    private LocalDate endDate;               // null = no end
    private LocalDate nextDueDate;

    @Builder.Default private Boolean isActive = true;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Builder.Default private PaymentMethod paymentMethod = PaymentMethod.CASH;

    @Builder.Default private String currency = "INR";

    @CreationTimestamp private LocalDateTime createdAt;

    public enum TransactionType { EXPENSE, INCOME }
    public enum Frequency       { DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY }
    public enum PaymentMethod   { CASH, CARD, UPI, BANK_TRANSFER, WALLET }
}
