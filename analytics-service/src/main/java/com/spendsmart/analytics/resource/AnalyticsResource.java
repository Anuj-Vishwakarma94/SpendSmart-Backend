package com.spendsmart.analytics.resource;

import com.spendsmart.analytics.dto.AnalyticsDto;
import com.spendsmart.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsResource {

    private final AnalyticsService analyticsService;

    private Long uid(Authentication a) { return (Long) a.getDetails(); }
    private int curMonth() { return LocalDate.now().getMonthValue(); }
    private int curYear()  { return LocalDate.now().getYear(); }

    @GetMapping("/summary/monthly")
    public ResponseEntity<AnalyticsDto.MonthlySummary> monthly(Authentication a,
            @RequestParam(defaultValue = "0") int month,
            @RequestParam(defaultValue = "0") int year) {
        return ResponseEntity.ok(analyticsService.getMonthlySummary(uid(a),
                month == 0 ? curMonth() : month, year == 0 ? curYear() : year));
    }

    @GetMapping("/summary/yearly")
    public ResponseEntity<AnalyticsDto.YearlySummary> yearly(Authentication a,
            @RequestParam(defaultValue = "0") int year) {
        return ResponseEntity.ok(analyticsService.getYearlySummary(uid(a), year == 0 ? curYear() : year));
    }

    @GetMapping("/breakdown/category")
    public ResponseEntity<Map<String, Double>> categoryBreakdown(Authentication a,
            @RequestParam(defaultValue = "0") int month,
            @RequestParam(defaultValue = "0") int year) {
        return ResponseEntity.ok(analyticsService.getExpenseBreakdownByCategory(uid(a),
                month == 0 ? curMonth() : month, year == 0 ? curYear() : year));
    }

    @GetMapping("/trend/income-expense")
    public ResponseEntity<List<AnalyticsDto.MonthlyTrend>> incomeTrend(Authentication a) {
        return ResponseEntity.ok(analyticsService.getIncomeVsExpenseTrend(uid(a)));
    }

    @GetMapping("/trend/savings-rate")
    public ResponseEntity<List<Double>> savingsRateTrend(Authentication a) {
        return ResponseEntity.ok(analyticsService.getSavingsRateTrend(uid(a)));
    }

    @GetMapping("/trend/daily")
    public ResponseEntity<List<AnalyticsDto.DailyTrend>> dailyTrend(Authentication a,
            @RequestParam(defaultValue = "0") int month,
            @RequestParam(defaultValue = "0") int year) {
        return ResponseEntity.ok(analyticsService.getDailyExpenseTrend(uid(a),
                month == 0 ? curMonth() : month, year == 0 ? curYear() : year));
    }

    @GetMapping("/categories/top")
    public ResponseEntity<List<Map<String, Object>>> topCategories(Authentication a,
            @RequestParam(defaultValue = "0") int month,
            @RequestParam(defaultValue = "0") int year) {
        return ResponseEntity.ok(analyticsService.getTopSpendingCategories(uid(a),
                month == 0 ? curMonth() : month, year == 0 ? curYear() : year));
    }

    @GetMapping("/cashflow")
    public ResponseEntity<AnalyticsDto.CashflowData> cashflow(Authentication a,
            @RequestParam(defaultValue = "0") int month,
            @RequestParam(defaultValue = "0") int year) {
        return ResponseEntity.ok(analyticsService.getCashflowData(uid(a),
                month == 0 ? curMonth() : month, year == 0 ? curYear() : year));
    }

    @GetMapping("/forecast")
    public ResponseEntity<AnalyticsDto.SpendingForecast> forecast(Authentication a) {
        return ResponseEntity.ok(analyticsService.getSpendingForecast(uid(a)));
    }

    @GetMapping("/health-score")
    public ResponseEntity<AnalyticsDto.FinancialHealthScore> healthScore(Authentication a) {
        return ResponseEntity.ok(analyticsService.getFinancialHealthScore(uid(a)));
    }

    @PostMapping("/snapshot")
    public ResponseEntity<?> generateSnapshot(Authentication a,
            @RequestParam(defaultValue = "0") int month,
            @RequestParam(defaultValue = "0") int year) {
        return ResponseEntity.ok(analyticsService.generateMonthlySnapshot(uid(a),
                month == 0 ? curMonth() : month, year == 0 ? curYear() : year));
    }
}
