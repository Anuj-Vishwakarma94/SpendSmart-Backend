package com.spendsmart.category.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;

@Entity
@Table(
    name = "categories",
    uniqueConstraints = {
        // a user cannot have two categories with the same name + type
        @UniqueConstraint(columnNames = {"userId", "name", "type"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long categoryId;

    /**
     * NULL for system-default categories (isDefault = true).
     * Set to the owner's userId for custom categories.
     */
    private Long userId;

    @NotBlank
    private String name;

    /**
     * EXPENSE or INCOME — ensures categories are only offered
     * for the matching transaction type.
     */
    @Enumerated(EnumType.STRING)
    @NotNull
    private CategoryType type;

    /**
     * Emoji or icon code rendered in the UI.
     * e.g. "🍔", "🚗", "fa-home"
     */
    @Builder.Default
    private String icon = "📦";

    /**
     * Hex colour code used for chart slices and category badges.
     * e.g. "#3fb950"
     */
    @Builder.Default
    private String colorCode = "#8b949e";

    /**
     * Optional per-category monthly spending cap.
     * Consumed by Budget-Service for budget-limit alerts.
     */
    private Double budgetLimit;

    /**
     * true  → pre-seeded system category, visible to all users.
     * false → custom category owned by userId.
     */
    @Builder.Default
    private Boolean isDefault = false;

    @CreationTimestamp
    private LocalDate createdAt;

    // ─── Enum ─────────────────────────────────────────────
    public enum CategoryType {
        EXPENSE, INCOME
    }
}
