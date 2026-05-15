package com.spendsmart.budget.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "budgets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long budgetId;

    @NotNull private Long userId;
    private Long categoryId;      // null = overall budget

    private String name;

    @Positive @NotNull
    private Double limitAmount;

    @Builder.Default private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Builder.Default private BudgetPeriod period = BudgetPeriod.MONTHLY;

    private LocalDate startDate;
    private LocalDate endDate;

    @Builder.Default private Double spentAmount = 0.0;

    /** 0–100 percentage at which alert fires, e.g. 80 */
    @Builder.Default private Integer alertThreshold = 80;

    @Builder.Default private Boolean isActive = true;

    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp  private LocalDateTime updatedAt;

    public enum BudgetPeriod { MONTHLY, WEEKLY, CUSTOM }
}
