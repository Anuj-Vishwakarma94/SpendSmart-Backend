package com.spendsmart.analytics.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

public class AnalyticsDto {

    @Data public static class MonthlySummary {
        private int year; private int month; private String period;
        private Double totalIncome; private Double totalExpenses;
        private Double netSavings; private Double savingsRate;
        private Double budgetUtilisation;
    }

    @Data public static class YearlySummary {
        private int year;
        private Double totalIncome; private Double totalExpenses;
        private Double netSavings; private Double avgSavingsRate;
        private List<MonthlySummary> monthlyBreakdown;
    }

    @Data public static class CategoryBreakdown {
        private Long categoryId; private String categoryName;
        private Double totalAmount; private Double percentage;
        private String colorCode; private String icon;
    }

    @Data public static class MonthlyTrend {
        private String month; private Double income; private Double expenses; private Double savings;
    }

    @Data public static class DailyTrend {
        private String date; private Double amount;
    }

    @Data public static class CashflowData {
        private String month; private Double inflow; private Double outflow; private Double net;
    }

    @Data public static class FinancialHealthScore {
        private int score;           // 0–100
        private Double savingsRate;
        private Double budgetAdherence;
        private Double expenseToIncomeRatio;
        private String grade;        // A/B/C/D/F
        private String message;
    }

    @Data public static class SpendingForecast {
        private Double projectedAmount;
        private Double threeMonthAvg;
        private String trend;        // INCREASING / DECREASING / STABLE
        private Double trendDelta;
    }
}
