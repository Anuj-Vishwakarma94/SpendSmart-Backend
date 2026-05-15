package com.spendsmart.expense.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;

public class ExpenseDto {

    // ─── Request DTOs ───────────────────────────────────────

    @Data
    public static class CreateExpenseRequest {
        @NotBlank(message = "Title is required")
        private String title;

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        private Double amount;

        private Long categoryId;

        @NotNull(message = "Date is required")
        private LocalDate date;

        private String paymentMethod = "CASH";
        private String notes;
        private String receiptUrl;
        private Boolean isRecurring = false;
        private String currency = "INR";
        private String type = "EXPENSE";
    }

    @Data
    public static class UpdateExpenseRequest {
        private String title;
        private Double amount;
        private Long categoryId;
        private LocalDate date;
        private String paymentMethod;
        private String notes;
        private String receiptUrl;
        private Boolean isRecurring;
        private String currency;
    }

    @Data
    public static class FilterRequest {
        private Long categoryId;
        private LocalDate startDate;
        private LocalDate endDate;
        private String paymentMethod;
        private Double minAmount;
        private Double maxAmount;
        private String keyword;
        private Integer month;
        private Integer year;
    }

    // ─── Response DTOs ──────────────────────────────────────

    @Data
    public static class ExpenseResponse {
        private Long expenseId;
        private Long userId;
        private Long categoryId;
        private String title;
        private Double amount;
        private String currency;
        private String type;
        private String paymentMethod;
        private LocalDate date;
        private String notes;
        private String receiptUrl;
        private Boolean isRecurring;
        private String createdAt;
        private String updatedAt;
    }

    @Data
    public static class ExpenseSummary {
        private Double totalAmount;
        private Long count;
        private String period;
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
