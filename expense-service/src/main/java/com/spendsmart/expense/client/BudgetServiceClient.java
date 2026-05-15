package com.spendsmart.expense.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.spendsmart.expense.config.FeignConfig;

import java.util.List;

@FeignClient(name = "budget-service", configuration = FeignConfig.class)
public interface BudgetServiceClient {

    @GetMapping("/api/budgets/category/{categoryId}")
    List<BudgetResponse> getByCategory(@RequestHeader("X-User-Id") Long userId, @PathVariable("categoryId") Long categoryId);

    @PutMapping("/api/budgets/{id}/spent")
    BudgetResponse updateSpent(@RequestHeader("X-User-Id") Long userId, @PathVariable("id") Long id, @RequestBody UpdateSpentRequest req);

    class UpdateSpentRequest {
        private Double delta;
        public UpdateSpentRequest() {}
        public UpdateSpentRequest(Double delta) { this.delta = delta; }
        public Double getDelta() { return delta; }
        public void setDelta(Double delta) { this.delta = delta; }
    }

    class BudgetResponse {
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

        // Getters and Setters
        public Long getBudgetId() { return budgetId; }
        public void setBudgetId(Long budgetId) { this.budgetId = budgetId; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Double getLimitAmount() { return limitAmount; }
        public void setLimitAmount(Double limitAmount) { this.limitAmount = limitAmount; }
        public Double getSpentAmount() { return spentAmount; }
        public void setSpentAmount(Double spentAmount) { this.spentAmount = spentAmount; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getPeriod() { return period; }
        public void setPeriod(String period) { this.period = period; }
        public String getStartDate() { return startDate; }
        public void setStartDate(String startDate) { this.startDate = startDate; }
        public String getEndDate() { return endDate; }
        public void setEndDate(String endDate) { this.endDate = endDate; }
        public Integer getAlertThreshold() { return alertThreshold; }
        public void setAlertThreshold(Integer alertThreshold) { this.alertThreshold = alertThreshold; }
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean isActive) { this.isActive = isActive; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }
}
