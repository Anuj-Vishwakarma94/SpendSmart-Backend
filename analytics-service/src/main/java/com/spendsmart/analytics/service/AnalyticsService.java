package com.spendsmart.analytics.service;

import com.spendsmart.analytics.dto.AnalyticsDto;
import com.spendsmart.analytics.entity.FinancialSnapshot;
import java.util.List;
import java.util.Map;

public interface AnalyticsService {
    FinancialSnapshot generateMonthlySnapshot(Long userId, int month, int year);
    AnalyticsDto.MonthlySummary getMonthlySummary(Long userId, int month, int year);
    AnalyticsDto.YearlySummary getYearlySummary(Long userId, int year);
    Map<String, Double> getExpenseBreakdownByCategory(Long userId, int month, int year);
    List<AnalyticsDto.MonthlyTrend> getIncomeVsExpenseTrend(Long userId);
    List<Double> getSavingsRateTrend(Long userId);
    List<Map<String, Object>> getTopSpendingCategories(Long userId, int month, int year);
    List<AnalyticsDto.DailyTrend> getDailyExpenseTrend(Long userId, int month, int year);
    AnalyticsDto.CashflowData getCashflowData(Long userId, int month, int year);
    AnalyticsDto.SpendingForecast getSpendingForecast(Long userId);
    AnalyticsDto.FinancialHealthScore getFinancialHealthScore(Long userId);
}
