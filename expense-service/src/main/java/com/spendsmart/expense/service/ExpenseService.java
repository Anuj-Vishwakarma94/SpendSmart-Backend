package com.spendsmart.expense.service;

import com.spendsmart.expense.dto.ExpenseDto;
import com.spendsmart.expense.entity.Expense;

import java.time.LocalDate;
import java.util.List;

public interface ExpenseService {

    ExpenseDto.ExpenseResponse addExpense(Long userId, ExpenseDto.CreateExpenseRequest request);

    ExpenseDto.ExpenseResponse getExpenseById(Long expenseId, Long userId);

    List<ExpenseDto.ExpenseResponse> getExpensesByUser(Long userId);

    List<ExpenseDto.ExpenseResponse> getExpensesByCategory(Long userId, Long categoryId);

    List<ExpenseDto.ExpenseResponse> getExpensesByDateRange(Long userId, LocalDate startDate, LocalDate endDate);

    List<ExpenseDto.ExpenseResponse> getExpensesByMonth(Long userId, int month, int year);

    List<ExpenseDto.ExpenseResponse> getExpensesByType(Long userId, String type);

    List<ExpenseDto.ExpenseResponse> searchExpenses(Long userId, String keyword);

    List<ExpenseDto.ExpenseResponse> getExpensesByAmountRange(Long userId, Double min, Double max);

    ExpenseDto.ExpenseResponse updateExpense(Long expenseId, Long userId, ExpenseDto.UpdateExpenseRequest request);

    ExpenseDto.MessageResponse deleteExpense(Long expenseId, Long userId);

    Double getTotalByUser(Long userId);

    Double getTotalByCategory(Long userId, Long categoryId);

    Double getTotalByMonth(Long userId, int month, int year);

    /** Admin-only: fetch all expenses across all users */
    List<ExpenseDto.ExpenseResponse> getAllExpenses();
}
