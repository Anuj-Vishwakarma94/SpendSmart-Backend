package com.spendsmart.analytics.serviceimpl;

import com.spendsmart.analytics.dto.AnalyticsDto;
import com.spendsmart.analytics.entity.FinancialSnapshot;
import com.spendsmart.analytics.repository.AnalyticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    @Mock private AnalyticsRepository analyticsRepository;
    @Mock private RestTemplate restTemplate;

    @InjectMocks private AnalyticsServiceImpl analyticsService;

    private static final Long USER_ID = 1L;
    private static final int MONTH = 4;
    private static final int YEAR = 2026;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(analyticsService, "expenseUrl", "http://localhost:8082");
        ReflectionTestUtils.setField(analyticsService, "incomeUrl",  "http://localhost:8083");
        ReflectionTestUtils.setField(analyticsService, "budgetUrl",  "http://localhost:8085");
    }

    // ─── Helpers ─────────────────────────────────────────────

    private Map<?, ?> totalMap(double value) {
        return Map.of("total", value);
    }

    private void stubIncome(double value) {
        when(restTemplate.getForObject(contains("/api/incomes/total/month"), eq(Map.class)))
                .thenReturn(totalMap(value));
    }

    private void stubExpenses(double value) {
        when(restTemplate.getForObject(contains("/api/expenses/total/month"), eq(Map.class)))
                .thenReturn(totalMap(value));
    }

    // ─── generateMonthlySnapshot ──────────────────────────────

    @Test
    @DisplayName("generateMonthlySnapshot: creates new snapshot when none exists")
    void generateMonthlySnapshot_newSnapshot() {
        stubIncome(10000.0);
        stubExpenses(6000.0);
        when(analyticsRepository.findByUserIdAndYearAndMonth(USER_ID, YEAR, MONTH))
                .thenReturn(Optional.empty());
        FinancialSnapshot saved = new FinancialSnapshot();
        saved.setUserId(USER_ID);
        saved.setTotalIncome(10000.0);
        saved.setTotalExpenses(6000.0);
        saved.setNetSavings(4000.0);
        saved.setSavingsRate(40.0);
        when(analyticsRepository.save(any(FinancialSnapshot.class))).thenReturn(saved);

        FinancialSnapshot result = analyticsService.generateMonthlySnapshot(USER_ID, MONTH, YEAR);

        assertThat(result).isNotNull();
        assertThat(result.getTotalIncome()).isEqualTo(10000.0);
        assertThat(result.getTotalExpenses()).isEqualTo(6000.0);
        assertThat(result.getNetSavings()).isEqualTo(4000.0);
        verify(analyticsRepository).save(any(FinancialSnapshot.class));
    }

    @Test
    @DisplayName("generateMonthlySnapshot: updates existing snapshot")
    void generateMonthlySnapshot_updatesExisting() {
        stubIncome(8000.0);
        stubExpenses(8000.0);
        FinancialSnapshot existing = new FinancialSnapshot();
        existing.setUserId(USER_ID);
        when(analyticsRepository.findByUserIdAndYearAndMonth(USER_ID, YEAR, MONTH))
                .thenReturn(Optional.of(existing));
        when(analyticsRepository.save(any(FinancialSnapshot.class))).thenAnswer(i -> i.getArgument(0));

        FinancialSnapshot result = analyticsService.generateMonthlySnapshot(USER_ID, MONTH, YEAR);

        assertThat(result.getNetSavings()).isEqualTo(0.0);
        assertThat(result.getSavingsRate()).isEqualTo(0.0);
        verify(analyticsRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("generateMonthlySnapshot: savings rate is 0 when income is 0")
    void generateMonthlySnapshot_zeroIncome() {
        stubIncome(0.0);
        stubExpenses(2000.0);
        when(analyticsRepository.findByUserIdAndYearAndMonth(any(), anyInt(), anyInt()))
                .thenReturn(Optional.empty());
        when(analyticsRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        FinancialSnapshot result = analyticsService.generateMonthlySnapshot(USER_ID, MONTH, YEAR);

        assertThat(result.getSavingsRate()).isEqualTo(0.0);
    }

    // ─── getMonthlySummary ────────────────────────────────────

    @Test
    @DisplayName("getMonthlySummary: returns correct calculations")
    void getMonthlySummary_correctCalculations() {
        stubIncome(5000.0);
        stubExpenses(2000.0);

        AnalyticsDto.MonthlySummary summary = analyticsService.getMonthlySummary(USER_ID, MONTH, YEAR);

        assertThat(summary).isNotNull();
        assertThat(summary.getTotalIncome()).isEqualTo(5000.0);
        assertThat(summary.getTotalExpenses()).isEqualTo(2000.0);
        assertThat(summary.getNetSavings()).isEqualTo(3000.0);
        assertThat(summary.getSavingsRate()).isEqualTo(60.0);
        assertThat(summary.getPeriod()).isEqualTo("2026-04");
        assertThat(summary.getMonth()).isEqualTo(MONTH);
        assertThat(summary.getYear()).isEqualTo(YEAR);
    }

    @Test
    @DisplayName("getMonthlySummary: zero income yields zero savings rate")
    void getMonthlySummary_zeroIncome() {
        stubIncome(0.0);
        stubExpenses(1000.0);

        AnalyticsDto.MonthlySummary summary = analyticsService.getMonthlySummary(USER_ID, MONTH, YEAR);

        assertThat(summary.getSavingsRate()).isEqualTo(0.0);
        assertThat(summary.getNetSavings()).isEqualTo(-1000.0);
    }

    @Test
    @DisplayName("getMonthlySummary: period is formatted correctly for single-digit month")
    void getMonthlySummary_periodFormatting() {
        stubIncome(3000.0);
        stubExpenses(1000.0);

        AnalyticsDto.MonthlySummary summary = analyticsService.getMonthlySummary(USER_ID, 3, 2026);

        assertThat(summary.getPeriod()).isEqualTo("2026-03");
    }

    // ─── getYearlySummary ─────────────────────────────────────

    @Test
    @DisplayName("getYearlySummary: aggregates 12 monthly summaries")
    void getYearlySummary_aggregates12Months() {
        when(restTemplate.getForObject(contains("/api/incomes/total/month"), eq(Map.class)))
                .thenReturn(totalMap(5000.0));
        when(restTemplate.getForObject(contains("/api/expenses/total/month"), eq(Map.class)))
                .thenReturn(totalMap(3000.0));

        AnalyticsDto.YearlySummary yearly = analyticsService.getYearlySummary(USER_ID, YEAR);

        assertThat(yearly).isNotNull();
        assertThat(yearly.getYear()).isEqualTo(YEAR);
        assertThat(yearly.getMonthlyBreakdown()).hasSize(12);
        assertThat(yearly.getTotalIncome()).isEqualTo(60000.0);
        assertThat(yearly.getTotalExpenses()).isEqualTo(36000.0);
        assertThat(yearly.getNetSavings()).isEqualTo(24000.0);
    }

    @Test
    @DisplayName("getYearlySummary: avg savings rate calculated correctly")
    void getYearlySummary_avgSavingsRate() {
        when(restTemplate.getForObject(contains("/api/incomes/total/month"), eq(Map.class)))
                .thenReturn(totalMap(10000.0));
        when(restTemplate.getForObject(contains("/api/expenses/total/month"), eq(Map.class)))
                .thenReturn(totalMap(5000.0));

        AnalyticsDto.YearlySummary yearly = analyticsService.getYearlySummary(USER_ID, YEAR);

        assertThat(yearly.getAvgSavingsRate()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("getYearlySummary: zero income gives 0 avg savings rate")
    void getYearlySummary_zeroIncome() {
        when(restTemplate.getForObject(contains("/api/incomes/total/month"), eq(Map.class)))
                .thenReturn(totalMap(0.0));
        when(restTemplate.getForObject(contains("/api/expenses/total/month"), eq(Map.class)))
                .thenReturn(totalMap(0.0));

        AnalyticsDto.YearlySummary yearly = analyticsService.getYearlySummary(USER_ID, YEAR);

        assertThat(yearly.getAvgSavingsRate()).isEqualTo(0.0);
    }

    // ─── getExpenseBreakdownByCategory ───────────────────────

    @Test
    @DisplayName("getExpenseBreakdownByCategory: returns 5 categories")
    void getExpenseBreakdownByCategory_returns5Categories() {
        Map<String, Double> breakdown = analyticsService.getExpenseBreakdownByCategory(USER_ID, MONTH, YEAR);

        assertThat(breakdown).isNotNull().hasSize(5);
        assertThat(breakdown).containsKeys("Food & Dining", "Transport", "Bills & Utilities", "Entertainment", "Other");
    }

    @Test
    @DisplayName("getExpenseBreakdownByCategory: all values are positive")
    void getExpenseBreakdownByCategory_positiveValues() {
        Map<String, Double> breakdown = analyticsService.getExpenseBreakdownByCategory(USER_ID, MONTH, YEAR);

        breakdown.values().forEach(v -> assertThat(v).isGreaterThan(0.0));
    }

    // ─── getIncomeVsExpenseTrend ──────────────────────────────

    @Test
    @DisplayName("getIncomeVsExpenseTrend: returns 12 months of trends")
    void getIncomeVsExpenseTrend_returns12Months() {
        when(restTemplate.getForObject(contains("/api/incomes/total/month"), eq(Map.class)))
                .thenReturn(totalMap(4000.0));
        when(restTemplate.getForObject(contains("/api/expenses/total/month"), eq(Map.class)))
                .thenReturn(totalMap(2500.0));

        List<AnalyticsDto.MonthlyTrend> trends = analyticsService.getIncomeVsExpenseTrend(USER_ID);

        assertThat(trends).hasSize(12);
    }

    @Test
    @DisplayName("getIncomeVsExpenseTrend: savings is income minus expenses")
    void getIncomeVsExpenseTrend_savingsCalculated() {
        when(restTemplate.getForObject(contains("/api/incomes/total/month"), eq(Map.class)))
                .thenReturn(totalMap(6000.0));
        when(restTemplate.getForObject(contains("/api/expenses/total/month"), eq(Map.class)))
                .thenReturn(totalMap(4000.0));

        List<AnalyticsDto.MonthlyTrend> trends = analyticsService.getIncomeVsExpenseTrend(USER_ID);

        trends.forEach(t -> {
            assertThat(t.getSavings()).isEqualTo(t.getIncome() - t.getExpenses());
            assertThat(t.getMonth()).isNotBlank();
        });
    }

    // ─── getSavingsRateTrend ──────────────────────────────────

    @Test
    @DisplayName("getSavingsRateTrend: returns 12 rates")
    void getSavingsRateTrend_returns12Rates() {
        when(restTemplate.getForObject(contains("/api/incomes/total/month"), eq(Map.class)))
                .thenReturn(totalMap(5000.0));
        when(restTemplate.getForObject(contains("/api/expenses/total/month"), eq(Map.class)))
                .thenReturn(totalMap(3000.0));

        List<Double> rates = analyticsService.getSavingsRateTrend(USER_ID);

        assertThat(rates).hasSize(12);
        rates.forEach(r -> assertThat(r).isEqualTo(40.0));
    }

    @Test
    @DisplayName("getSavingsRateTrend: zero income gives 0 rate")
    void getSavingsRateTrend_zeroIncome() {
        when(restTemplate.getForObject(contains("/api/incomes/total/month"), eq(Map.class)))
                .thenReturn(totalMap(0.0));
        when(restTemplate.getForObject(contains("/api/expenses/total/month"), eq(Map.class)))
                .thenReturn(totalMap(0.0));

        List<Double> rates = analyticsService.getSavingsRateTrend(USER_ID);

        rates.forEach(r -> assertThat(r).isEqualTo(0.0));
    }

    // ─── getTopSpendingCategories ─────────────────────────────

    @Test
    @DisplayName("getTopSpendingCategories: returns max 5 entries")
    void getTopSpendingCategories_maxFive() {
        List<Map<String, Object>> top = analyticsService.getTopSpendingCategories(USER_ID, MONTH, YEAR);

        assertThat(top).hasSizeLessThanOrEqualTo(5);
    }

    @Test
    @DisplayName("getTopSpendingCategories: entries have name, amount, percentage keys")
    void getTopSpendingCategories_hasRequiredKeys() {
        List<Map<String, Object>> top = analyticsService.getTopSpendingCategories(USER_ID, MONTH, YEAR);

        top.forEach(entry -> {
            assertThat(entry).containsKeys("name", "amount", "percentage");
        });
    }

    @Test
    @DisplayName("getTopSpendingCategories: sorted descending by amount")
    void getTopSpendingCategories_sortedDescending() {
        List<Map<String, Object>> top = analyticsService.getTopSpendingCategories(USER_ID, MONTH, YEAR);

        for (int i = 0; i < top.size() - 1; i++) {
            double current = (Double) top.get(i).get("amount");
            double next    = (Double) top.get(i + 1).get("amount");
            assertThat(current).isGreaterThanOrEqualTo(next);
        }
    }

    // ─── getDailyExpenseTrend ─────────────────────────────────

    @Test
    @DisplayName("getDailyExpenseTrend: April has 30 days")
    void getDailyExpenseTrend_aprilHas30Days() {
        List<AnalyticsDto.DailyTrend> daily = analyticsService.getDailyExpenseTrend(USER_ID, 4, 2026);

        assertThat(daily).hasSize(30);
    }

    @Test
    @DisplayName("getDailyExpenseTrend: February has 28 days in non-leap year")
    void getDailyExpenseTrend_februaryNonLeap() {
        List<AnalyticsDto.DailyTrend> daily = analyticsService.getDailyExpenseTrend(USER_ID, 2, 2025);

        assertThat(daily).hasSize(28);
    }

    @Test
    @DisplayName("getDailyExpenseTrend: February has 29 days in leap year")
    void getDailyExpenseTrend_februaryLeap() {
        List<AnalyticsDto.DailyTrend> daily = analyticsService.getDailyExpenseTrend(USER_ID, 2, 2024);

        assertThat(daily).hasSize(29);
    }

    @Test
    @DisplayName("getDailyExpenseTrend: date format is yyyy-MM-dd")
    void getDailyExpenseTrend_dateFormat() {
        List<AnalyticsDto.DailyTrend> daily = analyticsService.getDailyExpenseTrend(USER_ID, 4, 2026);

        assertThat(daily.get(0).getDate()).isEqualTo("2026-04-01");
        assertThat(daily.get(9).getDate()).isEqualTo("2026-04-10");
        assertThat(daily.get(29).getDate()).isEqualTo("2026-04-30");
    }

    // ─── getCashflowData ──────────────────────────────────────

    @Test
    @DisplayName("getCashflowData: net is inflow minus outflow")
    void getCashflowData_netCalculation() {
        stubIncome(8000.0);
        stubExpenses(5000.0);

        AnalyticsDto.CashflowData cf = analyticsService.getCashflowData(USER_ID, MONTH, YEAR);

        assertThat(cf.getInflow()).isEqualTo(8000.0);
        assertThat(cf.getOutflow()).isEqualTo(5000.0);
        assertThat(cf.getNet()).isEqualTo(3000.0);
        assertThat(cf.getMonth()).isEqualTo("2026-04");
    }

    @Test
    @DisplayName("getCashflowData: negative net when expenses exceed income")
    void getCashflowData_negativeNet() {
        stubIncome(2000.0);
        stubExpenses(5000.0);

        AnalyticsDto.CashflowData cf = analyticsService.getCashflowData(USER_ID, MONTH, YEAR);

        assertThat(cf.getNet()).isEqualTo(-3000.0);
    }

    // ─── getSpendingForecast ──────────────────────────────────

    @Test
    @DisplayName("getSpendingForecast: STABLE trend when delta within 200")
    void getSpendingForecast_stableTrend() {
        // m1=3000, m2=2900, m3=2950 → delta = 3000-2950 = 50 → STABLE
        when(restTemplate.getForObject(contains("/api/expenses/total/month"), eq(Map.class)))
                .thenReturn(totalMap(3000.0), totalMap(2900.0), totalMap(2950.0));

        AnalyticsDto.SpendingForecast forecast = analyticsService.getSpendingForecast(USER_ID);

        assertThat(forecast.getTrend()).isEqualTo("STABLE");
        assertThat(forecast.getProjectedAmount()).isGreaterThan(0);
    }

    @Test
    @DisplayName("getSpendingForecast: INCREASING trend when delta > 200")
    void getSpendingForecast_increasingTrend() {
        // m1=5000, m2=4000, m3=3000 → delta = 5000-3000 = 2000 → INCREASING
        when(restTemplate.getForObject(contains("/api/expenses/total/month"), eq(Map.class)))
                .thenReturn(totalMap(5000.0), totalMap(4000.0), totalMap(3000.0));

        AnalyticsDto.SpendingForecast forecast = analyticsService.getSpendingForecast(USER_ID);

        assertThat(forecast.getTrend()).isEqualTo("INCREASING");
    }

    @Test
    @DisplayName("getSpendingForecast: DECREASING trend when delta < -200")
    void getSpendingForecast_decreasingTrend() {
        // m1=2000, m2=3000, m3=4500 → delta = 2000-4500 = -2500 → DECREASING
        when(restTemplate.getForObject(contains("/api/expenses/total/month"), eq(Map.class)))
                .thenReturn(totalMap(2000.0), totalMap(3000.0), totalMap(4500.0));

        AnalyticsDto.SpendingForecast forecast = analyticsService.getSpendingForecast(USER_ID);

        assertThat(forecast.getTrend()).isEqualTo("DECREASING");
    }

    @Test
    @DisplayName("getSpendingForecast: threeMonthAvg is average of last 3 months")
    void getSpendingForecast_threeMonthAvg() {
        when(restTemplate.getForObject(contains("/api/expenses/total/month"), eq(Map.class)))
                .thenReturn(totalMap(3000.0), totalMap(3000.0), totalMap(3000.0));

        AnalyticsDto.SpendingForecast forecast = analyticsService.getSpendingForecast(USER_ID);

        assertThat(forecast.getThreeMonthAvg()).isEqualTo(3000.0);
    }

    // ─── getFinancialHealthScore ──────────────────────────────

    @Test
    @DisplayName("getFinancialHealthScore: grade A when score >= 80")
    void getFinancialHealthScore_gradeA() {
        // High income, low expenses → high savings rate → score likely A
        stubIncome(100000.0);
        stubExpenses(10000.0);

        AnalyticsDto.FinancialHealthScore hs = analyticsService.getFinancialHealthScore(USER_ID);

        assertThat(hs.getScore()).isBetween(0, 100);
        assertThat(hs.getGrade()).isIn("A", "B", "C", "D", "F");
        assertThat(hs.getMessage()).isNotBlank();
        assertThat(hs.getSavingsRate()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("getFinancialHealthScore: grade F when expenses greatly exceed income")
    void getFinancialHealthScore_gradeF() {
        stubIncome(1000.0);
        stubExpenses(9999.0);

        AnalyticsDto.FinancialHealthScore hs = analyticsService.getFinancialHealthScore(USER_ID);

        assertThat(hs.getScore()).isBetween(0, 100);
        assertThat(hs.getGrade()).isIn("D", "F");
    }

    @Test
    @DisplayName("getFinancialHealthScore: zero income clamps scores to minimum")
    void getFinancialHealthScore_zeroIncome() {
        stubIncome(0.0);
        stubExpenses(5000.0);

        AnalyticsDto.FinancialHealthScore hs = analyticsService.getFinancialHealthScore(USER_ID);

        assertThat(hs.getSavingsRate()).isEqualTo(0.0);
        assertThat(hs.getScore()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("getFinancialHealthScore: expenseToIncomeRatio is correct")
    void getFinancialHealthScore_expenseRatio() {
        stubIncome(10000.0);
        stubExpenses(4000.0);

        AnalyticsDto.FinancialHealthScore hs = analyticsService.getFinancialHealthScore(USER_ID);

        assertThat(hs.getExpenseToIncomeRatio()).isEqualTo(0.4);
    }

    // ─── RestTemplate error resilience ───────────────────────

    @Test
    @DisplayName("fetchMonthlyExpenses: returns 0.0 when REST call throws exception")
    void fetchMonthlyExpenses_resilientOnException() {
        when(restTemplate.getForObject(contains("/api/expenses/total/month"), eq(Map.class)))
                .thenThrow(new RuntimeException("Service down"));
        when(restTemplate.getForObject(contains("/api/incomes/total/month"), eq(Map.class)))
                .thenReturn(totalMap(5000.0));

        AnalyticsDto.MonthlySummary summary = analyticsService.getMonthlySummary(USER_ID, MONTH, YEAR);

        assertThat(summary.getTotalExpenses()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("fetchMonthlyIncome: returns 0.0 when REST call returns null")
    void fetchMonthlyIncome_resilientOnNull() {
        when(restTemplate.getForObject(contains("/api/incomes/total/month"), eq(Map.class)))
                .thenReturn(null);
        when(restTemplate.getForObject(contains("/api/expenses/total/month"), eq(Map.class)))
                .thenReturn(totalMap(2000.0));

        AnalyticsDto.MonthlySummary summary = analyticsService.getMonthlySummary(USER_ID, MONTH, YEAR);

        assertThat(summary.getTotalIncome()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("fetchMonthlyIncome: returns 0.0 when response map has no 'total' key")
    void fetchMonthlyIncome_missingTotalKey() {
        when(restTemplate.getForObject(contains("/api/incomes/total/month"), eq(Map.class)))
                .thenReturn(Map.of("amount", 5000.0));
        when(restTemplate.getForObject(contains("/api/expenses/total/month"), eq(Map.class)))
                .thenReturn(totalMap(1000.0));

        AnalyticsDto.MonthlySummary summary = analyticsService.getMonthlySummary(USER_ID, MONTH, YEAR);

        assertThat(summary.getTotalIncome()).isEqualTo(0.0);
    }
}
