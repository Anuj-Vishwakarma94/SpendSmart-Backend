package com.spendsmart.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

public class CategoryDto {

    // ─── Request DTOs ────────────────────────────────────────

    @Data
    public static class CreateCategoryRequest {

        @NotBlank(message = "Category name is required")
        private String name;

        @NotNull(message = "Category type is required (EXPENSE or INCOME)")
        private String type;   // "EXPENSE" | "INCOME"

        private String icon      = "📦";
        private String colorCode = "#8b949e";
        private Double budgetLimit;
    }

    @Data
    public static class UpdateCategoryRequest {
        private String name;
        private String icon;
        private String colorCode;
    }

    @Data
    public static class SetBudgetLimitRequest {
        private Double budgetLimit;   // null to remove limit
    }

    // ─── Response DTOs ────────────────────────────────────────

    @Data
    public static class CategoryResponse {
        private Long   categoryId;
        private Long   userId;
        private String name;
        private String type;
        private String icon;
        private String colorCode;
        private Double budgetLimit;
        private Boolean isDefault;
        private String  createdAt;
    }

    @Data
    public static class MessageResponse {
        private String  message;
        private boolean success;

        public MessageResponse(String message, boolean success) {
            this.message = message;
            this.success = success;
        }
    }

    @Data
    public static class CountResponse {
        private Long count;

        public CountResponse(Long count) { this.count = count; }
    }
}
