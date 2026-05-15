package com.spendsmart.budget.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

public class BudgetDto {

    @Data public static class CreateBudgetRequest {
        private String name;
        private Long categoryId;
        @NotNull @Positive private Double limitAmount;
        private String currency = "INR";
        private String period = "MONTHLY";
        private String startDate;
        private String endDate;
        private Integer alertThreshold = 80;
    }

    @Data public static class UpdateBudgetRequest {
        private String name;
        private Double limitAmount;
        private Integer alertThreshold;
        private String startDate;
        private String endDate;
        private Boolean isActive;
    }

    @Data public static class UpdateSpentRequest {
        @NotNull private Double delta;   // positive = add expense, negative = remove
    }

    @Data public static class BudgetResponse {
        private Long budgetId;
        private Long userId;
        private Long categoryId;
        private String name;
        private Double limitAmount;
        private Double spentAmount;
        private String currency;
        private String period;
        private String startDate;
        private String endDate;
        private Integer alertThreshold;
        private Boolean isActive;
        private String createdAt;
    }

    @Data public static class BudgetProgress {
        private Long budgetId;
        private String name;
        private Double limitAmount;
        private Double spentAmount;
        private Double remainingAmount;
        private Double percentageUsed;
        private Boolean thresholdBreached;
        private Boolean limitExceeded;
        private String status;   // SAFE | WARNING | EXCEEDED
    }

    @Data public static class MessageResponse {
        private String message;
        private boolean success;
        public MessageResponse(String message, boolean success) {
            this.message = message; this.success = success;
        }
    }
}
