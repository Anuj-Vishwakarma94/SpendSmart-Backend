package com.spendsmart.analytics.resource;

import com.spendsmart.analytics.dto.AnalyticsDto;
import com.spendsmart.analytics.entity.FinancialSnapshot;
import com.spendsmart.analytics.service.AnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsResourceTest {

    @Mock private AnalyticsService analyticsService;
    @Mock private Authentication authentication;

    @InjectMocks private AnalyticsResource analyticsResource;

    private static final Long USER_ID = 42L;

    @BeforeEach
    void setUp() {
        when(authentication.getDetails()).thenReturn(USER_ID);
    }

    // ─── monthly summary endpoint ─────────────────────────────

    @Test
    @DisplayName("GET /summary/monthly: returns 200 with summary")
    void monthly_returnsOk() {
        AnalyticsDto.MonthlySummary summary = new AnalyticsDto.MonthlySummary();
        summary.setTotalIncome(5000.0);
        when(analyticsService.getMonthlySummary(eq(USER_ID), anyInt(), anyInt())).thenReturn(summary);

        ResponseEntity<AnalyticsDto.MonthlySummary> response = analyticsResource.monthly(authentication, 0, 0);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTotalIncome()).isEqualTo(5000.0);
    }

    @Test
    @DisplayName("GET /summary/monthly: uses provided month and year when non-zero")
    void monthly_usesProvidedParams() {
        AnalyticsDto.MonthlySummary summary = new AnalyticsDto.MonthlySummary();
        when(analyticsService.getMonthlySummary(USER_ID, 3, 2025)).thenReturn(summary);

        ResponseEntity<AnalyticsDto.MonthlySummary> response = analyticsResource.monthly(authentication, 3, 2025);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        verify(analyticsService).getMonthlySummary(USER_ID, 3, 2025);
    }

    // ─── yearly summary endpoint ──────────────────────────────

    @Test
    @DisplayName("GET /summary/yearly: returns 200 with yearly summary")
    void yearly_returnsOk() {
        AnalyticsDto.YearlySummary ys = new AnalyticsDto.YearlySummary();
        ys.setYear(2026);
        when(analyticsService.getYearlySummary(eq(USER_ID), anyInt())).thenReturn(ys);

        ResponseEntity<AnalyticsDto.YearlySummary> response = analyticsResource.yearly(authentication, 0);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody().getYear()).isEqualTo(2026);
    }

    @Test
    @DisplayName("GET /summary/yearly: uses provided year when non-zero")
    void yearly_usesProvidedYear() {
        AnalyticsDto.YearlySummary ys = new AnalyticsDto.YearlySummary();
        when(analyticsService.getYearlySummary(USER_ID, 2024)).thenReturn(ys);

        analyticsResource.yearly(authentication, 2024);

        verify(analyticsService).getYearlySummary(USER_ID, 2024);
    }

    // ─── category breakdown endpoint ──────────────────────────

    @Test
    @DisplayName("GET /breakdown/category: returns 200 with category map")
    void categoryBreakdown_returnsOk() {
        Map<String, Double> breakdown = Map.of("Food", 1500.0, "Transport", 800.0);
        when(analyticsService.getExpenseBreakdownByCategory(eq(USER_ID), anyInt(), anyInt()))
                .thenReturn(breakdown);

        ResponseEntity<Map<String, Double>> response = analyticsResource.categoryBreakdown(authentication, 0, 0);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).containsKey("Food");
    }

    // ─── income vs expense trend endpoint ────────────────────

    @Test
    @DisplayName("GET /trend/income-expense: returns 200 with 12 trends")
    void incomeTrend_returnsOk() {
        List<AnalyticsDto.MonthlyTrend> trends = List.of(
                buildTrend("Jan 2026", 5000, 3000),
                buildTrend("Feb 2026", 5500, 3200)
        );
        when(analyticsService.getIncomeVsExpenseTrend(USER_ID)).thenReturn(trends);

        ResponseEntity<List<AnalyticsDto.MonthlyTrend>> response = analyticsResource.incomeTrend(authentication);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).hasSize(2);
    }

    // ─── savings rate trend endpoint ──────────────────────────

    @Test
    @DisplayName("GET /trend/savings-rate: returns 200 with rate list")
    void savingsRateTrend_returnsOk() {
        when(analyticsService.getSavingsRateTrend(USER_ID))
                .thenReturn(List.of(40.0, 35.0, 50.0));

        ResponseEntity<List<Double>> response = analyticsResource.savingsRateTrend(authentication);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).containsExactly(40.0, 35.0, 50.0);
    }

    // ─── daily trend endpoint ──────────────────────────────────

    @Test
    @DisplayName("GET /trend/daily: returns 200 with daily trends")
    void dailyTrend_returnsOk() {
        AnalyticsDto.DailyTrend dt = new AnalyticsDto.DailyTrend();
        dt.setDate("2026-04-01"); dt.setAmount(500.0);
        when(analyticsService.getDailyExpenseTrend(eq(USER_ID), anyInt(), anyInt()))
                .thenReturn(List.of(dt));

        ResponseEntity<List<AnalyticsDto.DailyTrend>> response = analyticsResource.dailyTrend(authentication, 0, 0);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getDate()).isEqualTo("2026-04-01");
    }

    @Test
    @DisplayName("GET /trend/daily: uses provided month and year when non-zero")
    void dailyTrend_usesProvidedParams() {
        when(analyticsService.getDailyExpenseTrend(USER_ID, 6, 2025)).thenReturn(List.of());

        analyticsResource.dailyTrend(authentication, 6, 2025);

        verify(analyticsService).getDailyExpenseTrend(USER_ID, 6, 2025);
    }

    // ─── top categories endpoint ──────────────────────────────

    @Test
    @DisplayName("GET /categories/top: returns 200 with top categories")
    void topCategories_returnsOk() {
        List<Map<String, Object>> cats = List.of(
                Map.of("name", "Food & Dining", "amount", 3000.0, "percentage", 35.0)
        );
        when(analyticsService.getTopSpendingCategories(eq(USER_ID), anyInt(), anyInt()))
                .thenReturn(cats);

        ResponseEntity<List<Map<String, Object>>> response = analyticsResource.topCategories(authentication, 0, 0);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).hasSize(1);
    }

    // ─── cashflow endpoint ────────────────────────────────────

    @Test
    @DisplayName("GET /cashflow: returns 200 with cashflow data")
    void cashflow_returnsOk() {
        AnalyticsDto.CashflowData cf = new AnalyticsDto.CashflowData();
        cf.setInflow(8000.0); cf.setOutflow(5000.0); cf.setNet(3000.0);
        when(analyticsService.getCashflowData(eq(USER_ID), anyInt(), anyInt())).thenReturn(cf);

        ResponseEntity<AnalyticsDto.CashflowData> response = analyticsResource.cashflow(authentication, 0, 0);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody().getNet()).isEqualTo(3000.0);
    }

    @Test
    @DisplayName("GET /cashflow: uses provided month and year when non-zero")
    void cashflow_usesProvidedParams() {
        AnalyticsDto.CashflowData cf = new AnalyticsDto.CashflowData();
        when(analyticsService.getCashflowData(USER_ID, 5, 2026)).thenReturn(cf);

        analyticsResource.cashflow(authentication, 5, 2026);

        verify(analyticsService).getCashflowData(USER_ID, 5, 2026);
    }

    // ─── forecast endpoint ────────────────────────────────────

    @Test
    @DisplayName("GET /forecast: returns 200 with forecast")
    void forecast_returnsOk() {
        AnalyticsDto.SpendingForecast sf = new AnalyticsDto.SpendingForecast();
        sf.setProjectedAmount(5000.0); sf.setTrend("STABLE");
        when(analyticsService.getSpendingForecast(USER_ID)).thenReturn(sf);

        ResponseEntity<AnalyticsDto.SpendingForecast> response = analyticsResource.forecast(authentication);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody().getTrend()).isEqualTo("STABLE");
    }

    // ─── health score endpoint ────────────────────────────────

    @Test
    @DisplayName("GET /health-score: returns 200 with health score")
    void healthScore_returnsOk() {
        AnalyticsDto.FinancialHealthScore hs = new AnalyticsDto.FinancialHealthScore();
        hs.setScore(75); hs.setGrade("B");
        when(analyticsService.getFinancialHealthScore(USER_ID)).thenReturn(hs);

        ResponseEntity<AnalyticsDto.FinancialHealthScore> response = analyticsResource.healthScore(authentication);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody().getScore()).isEqualTo(75);
        assertThat(response.getBody().getGrade()).isEqualTo("B");
    }

    // ─── snapshot endpoint ────────────────────────────────────

    @Test
    @DisplayName("POST /snapshot: returns 200 with created snapshot")
    void generateSnapshot_returnsOk() {
        FinancialSnapshot snap = new FinancialSnapshot();
        snap.setUserId(USER_ID);
        snap.setTotalIncome(7000.0);
        when(analyticsService.generateMonthlySnapshot(eq(USER_ID), anyInt(), anyInt())).thenReturn(snap);

        ResponseEntity<?> response = analyticsResource.generateSnapshot(authentication, 0, 0);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isInstanceOf(FinancialSnapshot.class);
        FinancialSnapshot body = (FinancialSnapshot) response.getBody();
        assertThat(body.getTotalIncome()).isEqualTo(7000.0);
    }

    @Test
    @DisplayName("POST /snapshot: uses provided month and year when non-zero")
    void generateSnapshot_usesProvidedParams() {
        FinancialSnapshot snap = new FinancialSnapshot();
        when(analyticsService.generateMonthlySnapshot(USER_ID, 2, 2025)).thenReturn(snap);

        analyticsResource.generateSnapshot(authentication, 2, 2025);

        verify(analyticsService).generateMonthlySnapshot(USER_ID, 2, 2025);
    }

    // ─── Helpers ──────────────────────────────────────────────

    private AnalyticsDto.MonthlyTrend buildTrend(String month, double income, double expenses) {
        AnalyticsDto.MonthlyTrend t = new AnalyticsDto.MonthlyTrend();
        t.setMonth(month); t.setIncome(income); t.setExpenses(expenses); t.setSavings(income - expenses);
        return t;
    }
}
