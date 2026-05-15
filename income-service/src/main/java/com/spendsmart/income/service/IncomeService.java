package com.spendsmart.income.service;

import com.spendsmart.income.dto.IncomeDto;

import java.time.LocalDate;
import java.util.List;

public interface IncomeService {

    // ─── CRUD ─────────────────────────────────────────────
    IncomeDto.IncomeResponse addIncome(Long userId, IncomeDto.CreateIncomeRequest request);

    IncomeDto.IncomeResponse getIncomeById(Long incomeId, Long userId);

    IncomeDto.IncomeResponse updateIncome(Long incomeId, Long userId, IncomeDto.UpdateIncomeRequest request);

    IncomeDto.MessageResponse deleteIncome(Long incomeId, Long userId);

    // ─── Queries ──────────────────────────────────────────
    List<IncomeDto.IncomeResponse> getIncomesByUser(Long userId);

    List<IncomeDto.IncomeResponse> getIncomesBySource(Long userId, String source);

    List<IncomeDto.IncomeResponse> getIncomesByDateRange(Long userId, LocalDate startDate, LocalDate endDate);

    List<IncomeDto.IncomeResponse> getIncomesByMonth(Long userId, int month, int year);

    List<IncomeDto.IncomeResponse> getIncomesByCategory(Long userId, Long categoryId);

    List<IncomeDto.IncomeResponse> searchIncomes(Long userId, String keyword);

    // ─── Recurring ────────────────────────────────────────
    List<IncomeDto.IncomeResponse> getRecurringIncomes(Long userId);

    // ─── Aggregations ─────────────────────────────────────
    Double getTotalIncomeByUser(Long userId);

    Double getTotalIncomeByMonth(Long userId, int month, int year);

    Double getTotalIncomeBySource(Long userId, String source);

    // ─── Breakdown ────────────────────────────────────────
    List<IncomeDto.IncomeBreakdownBySource> getBreakdownBySource(Long userId);
}
