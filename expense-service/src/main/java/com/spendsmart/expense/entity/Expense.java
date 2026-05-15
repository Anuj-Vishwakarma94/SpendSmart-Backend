package com.spendsmart.expense.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "expenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long expenseId;

    @NotNull
    private Long userId;

    private Long categoryId;

    @NotBlank
    private String title;

    @Positive
    @NotNull
    private Double amount;

    @Builder.Default
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ExpenseType type = ExpenseType.EXPENSE;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.CASH;

    @NotNull
    private LocalDate date;

    @Column(length = 1000)
    private String notes;

    private String receiptUrl;

    @Builder.Default
    private Boolean isRecurring = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum ExpenseType {
        EXPENSE, SPLIT
    }

    public enum PaymentMethod {
        CASH, CARD, UPI, BANK_TRANSFER, WALLET
    }
}
