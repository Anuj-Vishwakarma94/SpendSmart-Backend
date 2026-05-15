package com.spendsmart.budget.service;

import com.spendsmart.budget.dto.BudgetDto;
import java.util.List;

public interface BudgetService {
    BudgetDto.BudgetResponse createBudget(Long userId, BudgetDto.CreateBudgetRequest req);
    BudgetDto.BudgetResponse getBudgetById(Long budgetId, Long userId);
    List<BudgetDto.BudgetResponse> getBudgetsByUser(Long userId);
    List<BudgetDto.BudgetResponse> getActiveBudgets(Long userId);
    BudgetDto.BudgetResponse updateBudget(Long budgetId, Long userId, BudgetDto.UpdateBudgetRequest req);
    BudgetDto.MessageResponse deleteBudget(Long budgetId, Long userId);

    /** Called by Expense-Service via REST whenever an expense is added/edited/deleted. */
    BudgetDto.BudgetResponse updateSpentAmount(Long budgetId, Long userId, Double delta);

    BudgetDto.BudgetProgress getBudgetProgress(Long budgetId, Long userId);
    List<BudgetDto.BudgetProgress> getAllProgress(Long userId);
    List<String> checkBudgetAlerts(Long userId);

    /** Scheduled: reset spentAmount for all MONTHLY budgets on 1st of month. */
    void resetBudgetPeriods(Long userId, String period);

    List<BudgetDto.BudgetResponse> getBudgetsByCategory(Long userId, Long categoryId);
}
