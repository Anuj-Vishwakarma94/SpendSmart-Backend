package com.spendsmart.analytics.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "financial_snapshots",
       uniqueConstraints = @UniqueConstraint(columnNames = {"userId","year","month"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FinancialSnapshot {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long snapshotId;

    private Long   userId;
    private String period;       // e.g. "2026-04"
    private Integer year;
    private Integer month;

    private Double totalIncome;
    private Double totalExpenses;
    private Double netSavings;
    private Double savingsRate;  // percentage 0-100
    private String topCategory;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
