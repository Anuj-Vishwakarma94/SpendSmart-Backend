package com.spendsmart.income.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;

public class IncomeDto {

    // ─── Request DTOs ────────────────────────────────────────

    @Data
    public static class CreateIncomeRequest {

        @NotBlank(message = "Title is required")
        private String title;

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        private Double amount;

        private Long categoryId;

        @NotNull(message = "Date is required")
        private LocalDate date;

        /**
         * One of: SALARY, FREELANCE, BUSINESS, INVESTMENT, GIFT, OTHER
         */
        private String source = "OTHER";

        private String notes;

        private Boolean isRecurring = false;

        /**
         * Required when isRecurring = true.
         * One of: DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY
         */
        private String recurrencePeriod;

        /** Start date for recurring schedule (defaults to date if not provided) */
        private LocalDate startDate;

        /** Optional end date for the recurring schedule */
        private LocalDate endDate;

        private String currency = "INR";
    }

    @Data
    public static class UpdateIncomeRequest {
        private String title;
        private Double amount;
        private Long categoryId;
        private LocalDate date;
        private String source;
        private String notes;
        private Boolean isRecurring;
        private String recurrencePeriod;
        private String currency;
    }

    // ─── Response DTOs ────────────────────────────────────────

    @Data
    public static class IncomeResponse {
        private Long incomeId;
        private Long userId;
        private Long categoryId;
        private String title;
        private Double amount;
        private String currency;
        private String source;
        private LocalDate date;
        private String notes;
        private Boolean isRecurring;
        private String recurrencePeriod;
        private String createdAt;
        private String updatedAt;
    }

    @Data
    public static class IncomeSummary {
        private Double totalAmount;
        private Long count;
        private String period;
        private String source;
    }

    @Data
    public static class IncomeBreakdownBySource {
        private String source;
        private Double totalAmount;
        private Long count;
        private Double percentage;
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
