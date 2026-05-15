package com.spendsmart.analytics.serviceimpl;

import com.spendsmart.analytics.dto.AnalyticsDto;
import com.spendsmart.analytics.entity.FinancialSnapshot;
import com.spendsmart.analytics.repository.AnalyticsRepository;
import com.spendsmart.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final AnalyticsRepository analyticsRepository;
    private final RestTemplate restTemplate;

    @Value("${expense.service.url:http://localhost:8082}") private String expenseUrl;
    @Value("${income.service.url:http://localhost:8083}")  private String incomeUrl;
    @Value("${budget.service.url:http://localhost:8085}")  private String budgetUrl;

    // ─── Snapshot ─────────────────────────────────────────

    @Override @Transactional
    public FinancialSnapshot generateMonthlySnapshot(Long userId, int month, int year) {
        double income   = fetchMonthlyIncome(userId, month, year);
        double expenses = fetchMonthlyExpenses(userId, month, year);
        double savings  = income - expenses;
        double rate     = income > 0 ? Math.round((savings / income * 100) * 10.0) / 10.0 : 0;

        FinancialSnapshot snap = analyticsRepository
                .findByUserIdAndYearAndMonth(userId, year, month)
                .orElse(new FinancialSnapshot());

        snap.setUserId(userId);
        snap.setYear(year); snap.setMonth(month);
        snap.setPeriod(year + "-" + String.format("%02d", month));
        snap.setTotalIncome(income); snap.setTotalExpenses(expenses);
        snap.setNetSavings(savings); snap.setSavingsRate(rate);
        return analyticsRepository.save(snap);
    }

    // ─── Monthly Summary ──────────────────────────────────

    @Override
    public AnalyticsDto.MonthlySummary getMonthlySummary(Long userId, int month, int year) {
        double income   = fetchMonthlyIncome(userId, month, year);
        double expenses = fetchMonthlyExpenses(userId, month, year);
        double savings  = income - expenses;
        double rate     = income > 0 ? Math.round((savings / income * 100) * 10.0) / 10.0 : 0;

        AnalyticsDto.MonthlySummary s = new AnalyticsDto.MonthlySummary();
        s.setYear(year); s.setMonth(month);
        s.setPeriod(year + "-" + String.format("%02d", month));
        s.setTotalIncome(income); s.setTotalExpenses(expenses);
        s.setNetSavings(savings); s.setSavingsRate(rate);
        return s;
    }

    // ─── Yearly Summary ───────────────────────────────────

    @Override
    public AnalyticsDto.YearlySummary getYearlySummary(Long userId, int year) {
        List<AnalyticsDto.MonthlySummary> months = new ArrayList<>();
        double totalIncome = 0, totalExpenses = 0;

        for (int m = 1; m <= 12; m++) {
            AnalyticsDto.MonthlySummary ms = getMonthlySummary(userId, m, year);
            months.add(ms);
            totalIncome   += ms.getTotalIncome();
            totalExpenses += ms.getTotalExpenses();
        }

        AnalyticsDto.YearlySummary ys = new AnalyticsDto.YearlySummary();
        ys.setYear(year); ys.setTotalIncome(totalIncome); ys.setTotalExpenses(totalExpenses);
        ys.setNetSavings(totalIncome - totalExpenses);
        ys.setAvgSavingsRate(totalIncome > 0
                ? Math.round(((totalIncome - totalExpenses) / totalIncome * 100) * 10.0) / 10.0 : 0);
        ys.setMonthlyBreakdown(months);
        return ys;
    }

    // ─── Category Breakdown ───────────────────────────────

    @Override
    public Map<String, Double> getExpenseBreakdownByCategory(Long userId, int month, int year) {
        // Returns category-name → amount map (simplified; real impl calls expense-service)
        Map<String, Double> breakdown = new LinkedHashMap<>();
        breakdown.put("Food & Dining",     fetchRandomSample(500, 3000));
        breakdown.put("Transport",         fetchRandomSample(200, 1500));
        breakdown.put("Bills & Utilities", fetchRandomSample(1000, 5000));
        breakdown.put("Entertainment",     fetchRandomSample(100, 2000));
        breakdown.put("Other",             fetchRandomSample(100, 1000));
        return breakdown;
    }

    // ─── Income vs Expense Trend (12 months) ─────────────

    @Override
    public List<AnalyticsDto.MonthlyTrend> getIncomeVsExpenseTrend(Long userId) {
        List<AnalyticsDto.MonthlyTrend> result = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (int i = 11; i >= 0; i--) {
            YearMonth ym = YearMonth.from(now).minusMonths(i);
            double inc = fetchMonthlyIncome(userId, ym.getMonthValue(), ym.getYear());
            double exp = fetchMonthlyExpenses(userId, ym.getMonthValue(), ym.getYear());
            AnalyticsDto.MonthlyTrend t = new AnalyticsDto.MonthlyTrend();
            t.setMonth(ym.getMonth().toString().substring(0, 3) + " " + ym.getYear());
            t.setIncome(inc); t.setExpenses(exp); t.setSavings(inc - exp);
            result.add(t);
        }
        return result;
    }

    // ─── Savings Rate Trend ───────────────────────────────

    @Override
    public List<Double> getSavingsRateTrend(Long userId) {
        return getIncomeVsExpenseTrend(userId).stream()
                .map(t -> t.getIncome() > 0
                        ? Math.round((t.getSavings() / t.getIncome() * 100) * 10.0) / 10.0 : 0.0)
                .collect(Collectors.toList());
    }

    // ─── Top Spending Categories ──────────────────────────

    @Override
    public List<Map<String, Object>> getTopSpendingCategories(Long userId, int month, int year) {
        Map<String, Double> breakdown = getExpenseBreakdownByCategory(userId, month, year);
        double total = breakdown.values().stream().mapToDouble(Double::doubleValue).sum();
        return breakdown.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", e.getKey());
                    m.put("amount", e.getValue());
                    m.put("percentage", total > 0 ? Math.round(e.getValue() / total * 1000) / 10.0 : 0);
                    return m;
                })
                .collect(Collectors.toList());
    }

    // ─── Daily Expense Trend ──────────────────────────────

    @Override
    public List<AnalyticsDto.DailyTrend> getDailyExpenseTrend(Long userId, int month, int year) {
        YearMonth ym = YearMonth.of(year, month);
        List<AnalyticsDto.DailyTrend> result = new ArrayList<>();
        for (int d = 1; d <= ym.lengthOfMonth(); d++) {
            AnalyticsDto.DailyTrend dt = new AnalyticsDto.DailyTrend();
            dt.setDate(year + "-" + String.format("%02d", month) + "-" + String.format("%02d", d));
            dt.setAmount(0.0); // expense-service would populate this
            result.add(dt);
        }
        return result;
    }

    // ─── Cashflow ─────────────────────────────────────────

    @Override
    public AnalyticsDto.CashflowData getCashflowData(Long userId, int month, int year) {
        double income   = fetchMonthlyIncome(userId, month, year);
        double expenses = fetchMonthlyExpenses(userId, month, year);
        AnalyticsDto.CashflowData cf = new AnalyticsDto.CashflowData();
        cf.setMonth(year + "-" + String.format("%02d", month));
        cf.setInflow(income); cf.setOutflow(expenses); cf.setNet(income - expenses);
        return cf;
    }

    // ─── Spending Forecast ────────────────────────────────

    @Override
    public AnalyticsDto.SpendingForecast getSpendingForecast(Long userId) {
        LocalDate now = LocalDate.now();
        double m1 = fetchMonthlyExpenses(userId, now.minusMonths(1).getMonthValue(), now.minusMonths(1).getYear());
        double m2 = fetchMonthlyExpenses(userId, now.minusMonths(2).getMonthValue(), now.minusMonths(2).getYear());
        double m3 = fetchMonthlyExpenses(userId, now.minusMonths(3).getMonthValue(), now.minusMonths(3).getYear());

        double avg = (m1 + m2 + m3) / 3.0;
        // Exponential smoothing: weight recent months more
        double weighted = (m1 * 0.5 + m2 * 0.3 + m3 * 0.2);
        double projected = Math.round(weighted * 10.0) / 10.0;
        double delta = m1 - m3;

        AnalyticsDto.SpendingForecast f = new AnalyticsDto.SpendingForecast();
        f.setProjectedAmount(projected);
        f.setThreeMonthAvg(Math.round(avg * 10.0) / 10.0);
        f.setTrendDelta(Math.round(delta * 10.0) / 10.0);
        f.setTrend(delta > 200 ? "INCREASING" : delta < -200 ? "DECREASING" : "STABLE");
        return f;
    }

    // ─── Financial Health Score ───────────────────────────
    // Composite: savings rate 40% + budget adherence 40% + expense/income ratio 20%

    @Override
    public AnalyticsDto.FinancialHealthScore getFinancialHealthScore(Long userId) {
        LocalDate now = LocalDate.now();
        double income   = fetchMonthlyIncome(userId, now.getMonthValue(), now.getYear());
        double expenses = fetchMonthlyExpenses(userId, now.getMonthValue(), now.getYear());

        double savingsRate   = income > 0 ? Math.max(0, Math.min(100, (income - expenses) / income * 100)) : 0;
        double expRatio      = income > 0 ? expenses / income : 1.0;
        double budgetAdhere  = 85.0; // placeholder — real: query budget-service alerts

        double savScore   = Math.min(40, savingsRate * 0.40);
        double budScore   = (budgetAdhere / 100.0) * 40;
        double ratioScore = Math.max(0, (1 - expRatio)) * 20;
        int total = (int) Math.round(savScore + budScore + ratioScore);

        AnalyticsDto.FinancialHealthScore hs = new AnalyticsDto.FinancialHealthScore();
        hs.setScore(Math.max(0, Math.min(100, total)));
        hs.setSavingsRate(Math.round(savingsRate * 10.0) / 10.0);
        hs.setBudgetAdherence(budgetAdhere);
        hs.setExpenseToIncomeRatio(Math.round(expRatio * 100.0) / 100.0);
        hs.setGrade(total >= 80 ? "A" : total >= 65 ? "B" : total >= 50 ? "C" : total >= 35 ? "D" : "F");
        hs.setMessage(total >= 80 ? "Excellent! You're saving well and within budget."
                    : total >= 65 ? "Good financial health. Keep it up!"
                    : total >= 50 ? "Fair. Consider reducing discretionary spending."
                    : total >= 35 ? "Needs attention. Review your budget limits."
                    : "Critical. Your expenses significantly exceed income.");
        return hs;
    }

    // ─── Remote Fetch Helpers ─────────────────────────────

    private double fetchMonthlyExpenses(Long userId, int month, int year) {
        try {
            String url = expenseUrl + "/api/expenses/total/month?month=" + month + "&year=" + year;
            Map<?,?> r = restTemplate.getForObject(url, Map.class);
            Object v = r != null ? r.get("total") : null;
            return v instanceof Number ? ((Number) v).doubleValue() : 0.0;
        } catch (Exception e) { return 0.0; }
    }

    private double fetchMonthlyIncome(Long userId, int month, int year) {
        try {
            String url = incomeUrl + "/api/incomes/total/month?month=" + month + "&year=" + year;
            Map<?,?> r = restTemplate.getForObject(url, Map.class);
            Object v = r != null ? r.get("total") : null;
            return v instanceof Number ? ((Number) v).doubleValue() : 0.0;
        } catch (Exception e) { return 0.0; }
    }

    private double fetchRandomSample(double min, double max) {
        return Math.round((min + Math.random() * (max - min)) * 100.0) / 100.0;
    }
}
