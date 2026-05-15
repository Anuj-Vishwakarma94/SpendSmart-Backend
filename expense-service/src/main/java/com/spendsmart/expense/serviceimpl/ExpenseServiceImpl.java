package com.spendsmart.expense.serviceimpl;

import com.spendsmart.expense.dto.ExpenseDto;
import com.spendsmart.expense.entity.Expense;
import com.spendsmart.expense.repository.ExpenseRepository;
import com.spendsmart.expense.service.ExpenseService;
import com.spendsmart.expense.client.BudgetServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final BudgetServiceClient budgetServiceClient;

    @Override
    @Transactional
    public ExpenseDto.ExpenseResponse addExpense(Long userId, ExpenseDto.CreateExpenseRequest request) {
        Expense expense = Expense.builder()
                .userId(userId)
                .title(request.getTitle())
                .amount(request.getAmount())
                .categoryId(request.getCategoryId())
                .date(request.getDate())
                .paymentMethod(parsePaymentMethod(request.getPaymentMethod()))
                .notes(request.getNotes())
                .receiptUrl(request.getReceiptUrl())
                .isRecurring(request.getIsRecurring() != null ? request.getIsRecurring() : false)
                .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                .type(parseExpenseType(request.getType()))
                .build();

        Expense savedExpense = expenseRepository.save(expense);
        updateBudgetsForCategory(userId, savedExpense.getCategoryId(), savedExpense.getAmount());
        return toResponse(savedExpense);
    }

    @Override
    public ExpenseDto.ExpenseResponse getExpenseById(Long expenseId, Long userId) {
        Expense expense = expenseRepository.findByExpenseId(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found: " + expenseId));
        validateOwnership(expense, userId);
        return toResponse(expense);
    }

    @Override
    public List<ExpenseDto.ExpenseResponse> getExpensesByUser(Long userId) {
        return expenseRepository.findByUserIdOrderByDateDesc(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<ExpenseDto.ExpenseResponse> getExpensesByCategory(Long userId, Long categoryId) {
        return expenseRepository.findByUserIdAndCategoryId(userId, categoryId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<ExpenseDto.ExpenseResponse> getExpensesByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return expenseRepository.findByUserIdAndDateBetweenOrderByDateDesc(userId, startDate, endDate)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<ExpenseDto.ExpenseResponse> getExpensesByMonth(Long userId, int month, int year) {
        return expenseRepository.findByUserIdAndMonth(userId, month, year)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<ExpenseDto.ExpenseResponse> getExpensesByType(Long userId, String type) {
        return expenseRepository.findByUserIdAndType(userId, parseExpenseType(type))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<ExpenseDto.ExpenseResponse> searchExpenses(Long userId, String keyword) {
        return expenseRepository.searchByKeyword(userId, keyword)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<ExpenseDto.ExpenseResponse> getExpensesByAmountRange(Long userId, Double min, Double max) {
        return expenseRepository.findByUserIdAndAmountBetween(userId, min, max)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ExpenseDto.ExpenseResponse updateExpense(Long expenseId, Long userId, ExpenseDto.UpdateExpenseRequest request) {
        Expense expense = expenseRepository.findByExpenseId(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found: " + expenseId));
        validateOwnership(expense, userId);

        Double oldAmount = expense.getAmount();
        Long oldCategoryId = expense.getCategoryId();

        if (request.getTitle() != null) expense.setTitle(request.getTitle());
        if (request.getAmount() != null) expense.setAmount(request.getAmount());
        if (request.getCategoryId() != null) expense.setCategoryId(request.getCategoryId());
        if (request.getDate() != null) expense.setDate(request.getDate());
        if (request.getPaymentMethod() != null) expense.setPaymentMethod(parsePaymentMethod(request.getPaymentMethod()));
        if (request.getNotes() != null) expense.setNotes(request.getNotes());
        if (request.getReceiptUrl() != null) expense.setReceiptUrl(request.getReceiptUrl());
        if (request.getIsRecurring() != null) expense.setIsRecurring(request.getIsRecurring());
        if (request.getCurrency() != null) expense.setCurrency(request.getCurrency());

        Expense updatedExpense = expenseRepository.save(expense);

        // Handle budget updates if amount or category changed
        if (!oldAmount.equals(updatedExpense.getAmount()) || !oldCategoryId.equals(updatedExpense.getCategoryId())) {
            if (oldCategoryId.equals(updatedExpense.getCategoryId())) {
                // Same category, just amount changed
                Double delta = updatedExpense.getAmount() - oldAmount;
                updateBudgetsForCategory(userId, updatedExpense.getCategoryId(), delta);
            } else {
                // Category changed: remove from old, add to new
                updateBudgetsForCategory(userId, oldCategoryId, -oldAmount);
                updateBudgetsForCategory(userId, updatedExpense.getCategoryId(), updatedExpense.getAmount());
            }
        }

        return toResponse(updatedExpense);
    }

    @Override
    @Transactional
    public ExpenseDto.MessageResponse deleteExpense(Long expenseId, Long userId) {
        Expense expense = expenseRepository.findByExpenseId(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found: " + expenseId));
        validateOwnership(expense, userId);
        expenseRepository.deleteByExpenseId(expenseId);
        
        // Remove amount from budget
        updateBudgetsForCategory(userId, expense.getCategoryId(), -expense.getAmount());
        
        return new ExpenseDto.MessageResponse("Expense deleted successfully", true);
    }

    @Override
    public Double getTotalByUser(Long userId) {
        Double total = expenseRepository.sumAmountByUserId(userId);
        return total != null ? total : 0.0;
    }

    @Override
    public Double getTotalByCategory(Long userId, Long categoryId) {
        Double total = expenseRepository.sumAmountByUserIdAndCategoryId(userId, categoryId);
        return total != null ? total : 0.0;
    }

    @Override
    public Double getTotalByMonth(Long userId, int month, int year) {
        Double total = expenseRepository.sumAmountByUserIdAndMonth(userId, month, year);
        return total != null ? total : 0.0;
    }

    @Override
    public List<ExpenseDto.ExpenseResponse> getAllExpenses() {
        return expenseRepository.findAll()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ─── Helpers ──────────────────────────────────────────────

    private void updateBudgetsForCategory(Long userId, Long categoryId, Double delta) {
        if (categoryId == null || delta == null || delta == 0.0) return;
        try {
            List<BudgetServiceClient.BudgetResponse> budgets = budgetServiceClient.getByCategory(userId, categoryId);
            if (budgets != null) {
                for (BudgetServiceClient.BudgetResponse budget : budgets) {
                    if (Boolean.TRUE.equals(budget.getIsActive())) {
                        budgetServiceClient.updateSpent(userId, budget.getBudgetId(), new BudgetServiceClient.UpdateSpentRequest(delta));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to update budget for category {} and user {}", categoryId, userId, e);
            // Non-blocking: we don't want to fail the expense operation if budget service is down
        }
    }

    private void validateOwnership(Expense expense, Long userId) {
        if (!expense.getUserId().equals(userId)) {
            throw new SecurityException("Access denied: expense does not belong to user");
        }
    }

    private Expense.PaymentMethod parsePaymentMethod(String method) {
        try {
            return method != null ? Expense.PaymentMethod.valueOf(method.toUpperCase()) : Expense.PaymentMethod.CASH;
        } catch (IllegalArgumentException e) {
            return Expense.PaymentMethod.CASH;
        }
    }

    private Expense.ExpenseType parseExpenseType(String type) {
        try {
            return type != null ? Expense.ExpenseType.valueOf(type.toUpperCase()) : Expense.ExpenseType.EXPENSE;
        } catch (IllegalArgumentException e) {
            return Expense.ExpenseType.EXPENSE;
        }
    }

    private ExpenseDto.ExpenseResponse toResponse(Expense expense) {
        ExpenseDto.ExpenseResponse res = new ExpenseDto.ExpenseResponse();
        res.setExpenseId(expense.getExpenseId());
        res.setUserId(expense.getUserId());
        res.setCategoryId(expense.getCategoryId());
        res.setTitle(expense.getTitle());
        res.setAmount(expense.getAmount());
        res.setCurrency(expense.getCurrency());
        res.setType(expense.getType().name());
        res.setPaymentMethod(expense.getPaymentMethod().name());
        res.setDate(expense.getDate());
        res.setNotes(expense.getNotes());
        res.setReceiptUrl(expense.getReceiptUrl());
        res.setIsRecurring(expense.getIsRecurring());
        res.setCreatedAt(expense.getCreatedAt() != null ? expense.getCreatedAt().toString() : null);
        res.setUpdatedAt(expense.getUpdatedAt() != null ? expense.getUpdatedAt().toString() : null);
        return res;
    }
}
