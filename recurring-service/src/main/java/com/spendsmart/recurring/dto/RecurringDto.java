package com.spendsmart.recurring.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

public class RecurringDto {

    @Data public static class CreateRequest {
        @NotBlank  private String title;
        @NotNull @Positive private Double amount;
        @NotNull   private String type;        // EXPENSE | INCOME
        @NotNull   private String frequency;   // DAILY|WEEKLY|MONTHLY|QUARTERLY|YEARLY
        @NotNull   private String startDate;
        private String endDate;
        private Long categoryId;
        private String paymentMethod = "CASH";
        private String description;
        private String currency = "INR";
    }

    @Data public static class UpdateRequest {
        private String title;
        private Double amount;
        private String frequency;
        private String endDate;
        private Long categoryId;
        private String paymentMethod;
        private String description;
        private Boolean isActive;
    }

    @Data public static class RecurringResponse {
        private Long recurringId;
        private Long userId;
        private Long categoryId;
        private String title;
        private Double amount;
        private String currency;
        private String type;
        private String frequency;
        private String startDate;
        private String endDate;
        private String nextDueDate;
        private Boolean isActive;
        private String description;
        private String paymentMethod;
        private String createdAt;
        // days until next due — convenience field
        private Long daysUntilDue;
    }

    @Data public static class MessageResponse {
        private String message;
        private boolean success;
        public MessageResponse(String message, boolean success) { this.message = message; this.success = success; }
    }
}
