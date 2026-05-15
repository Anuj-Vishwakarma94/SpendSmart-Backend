package com.spendsmart.income.entity;

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
@Table(name = "incomes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Income {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long incomeId;

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

    /**
     * Origin of the income entry.
     * Drives the income breakdown charts in Analytics-Service.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private IncomeSource source = IncomeSource.OTHER;

    @NotNull
    private LocalDate date;

    @Column(length = 1000)
    private String notes;

    @Builder.Default
    private Boolean isRecurring = false;

    /**
     * Frequency used when isRecurring = true.
     * e.g. MONTHLY for a salary, YEARLY for a bonus.
     */
    @Enumerated(EnumType.STRING)
    private RecurrencePeriod recurrencePeriod;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ─── Enums ────────────────────────────────────────────

    public enum IncomeSource {
        SALARY,
        FREELANCE,
        BUSINESS,
        INVESTMENT,
        GIFT,
        OTHER
    }

    public enum RecurrencePeriod {
        DAILY,
        WEEKLY,
        MONTHLY,
        QUARTERLY,
        YEARLY
    }
}
