package com.spendsmart.income.serviceimpl;

import com.spendsmart.income.dto.IncomeDto;
import com.spendsmart.income.entity.Income;
import com.spendsmart.income.repository.IncomeRepository;
import com.spendsmart.income.service.IncomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IncomeServiceImpl implements IncomeService {

    private final IncomeRepository incomeRepository;

    // ─── CRUD ─────────────────────────────────────────────

    @Override
    @Transactional
    public IncomeDto.IncomeResponse addIncome(Long userId, IncomeDto.CreateIncomeRequest request) {
        // Validate recurrencePeriod is provided when isRecurring = true
        if (Boolean.TRUE.equals(request.getIsRecurring()) && request.getRecurrencePeriod() == null) {
            throw new IllegalArgumentException("recurrencePeriod is required when isRecurring is true");
        }

        Income income = Income.builder()
                .userId(userId)
                .title(request.getTitle())
                .amount(request.getAmount())
                .categoryId(request.getCategoryId())
                .date(request.getDate())
                .source(parseSource(request.getSource()))
                .notes(request.getNotes())
                .isRecurring(Boolean.TRUE.equals(request.getIsRecurring()))
                .recurrencePeriod(parseRecurrencePeriod(request.getRecurrencePeriod()))
                .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                .build();

        return toResponse(incomeRepository.save(income));
    }

    @Override
    public IncomeDto.IncomeResponse getIncomeById(Long incomeId, Long userId) {
        Income income = findAndValidate(incomeId, userId);
        return toResponse(income);
    }

    @Override
    @Transactional
    public IncomeDto.IncomeResponse updateIncome(Long incomeId, Long userId,
                                                  IncomeDto.UpdateIncomeRequest request) {
        Income income = findAndValidate(incomeId, userId);

        if (request.getTitle() != null)            income.setTitle(request.getTitle());
        if (request.getAmount() != null)           income.setAmount(request.getAmount());
        if (request.getCategoryId() != null)       income.setCategoryId(request.getCategoryId());
        if (request.getDate() != null)             income.setDate(request.getDate());
        if (request.getSource() != null)           income.setSource(parseSource(request.getSource()));
        if (request.getNotes() != null)            income.setNotes(request.getNotes());
        if (request.getIsRecurring() != null)      income.setIsRecurring(request.getIsRecurring());
        if (request.getRecurrencePeriod() != null) income.setRecurrencePeriod(parseRecurrencePeriod(request.getRecurrencePeriod()));
        if (request.getCurrency() != null)         income.setCurrency(request.getCurrency());

        return toResponse(incomeRepository.save(income));
    }

    @Override
    @Transactional
    public IncomeDto.MessageResponse deleteIncome(Long incomeId, Long userId) {
        findAndValidate(incomeId, userId);
        incomeRepository.deleteByIncomeId(incomeId);
        return new IncomeDto.MessageResponse("Income entry deleted successfully", true);
    }

    // ─── Queries ──────────────────────────────────────────

    @Override
    public List<IncomeDto.IncomeResponse> getIncomesByUser(Long userId) {
        return incomeRepository.findByUserIdOrderByDateDesc(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<IncomeDto.IncomeResponse> getIncomesBySource(Long userId, String source) {
        return incomeRepository.findByUserIdAndSource(userId, parseSource(source))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<IncomeDto.IncomeResponse> getIncomesByDateRange(Long userId,
                                                                 LocalDate startDate,
                                                                 LocalDate endDate) {
        return incomeRepository.findByUserIdAndDateBetweenOrderByDateDesc(userId, startDate, endDate)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<IncomeDto.IncomeResponse> getIncomesByMonth(Long userId, int month, int year) {
        return incomeRepository.findByUserIdAndMonth(userId, month, year)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<IncomeDto.IncomeResponse> getIncomesByCategory(Long userId, Long categoryId) {
        return incomeRepository.findByUserIdAndCategoryId(userId, categoryId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<IncomeDto.IncomeResponse> searchIncomes(Long userId, String keyword) {
        return incomeRepository.searchByKeyword(userId, keyword)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ─── Recurring ────────────────────────────────────────

    @Override
    public List<IncomeDto.IncomeResponse> getRecurringIncomes(Long userId) {
        return incomeRepository.findByUserIdAndIsRecurring(userId, true)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ─── Aggregations ─────────────────────────────────────

    @Override
    public Double getTotalIncomeByUser(Long userId) {
        Double total = incomeRepository.sumAmountByUserId(userId);
        return total != null ? total : 0.0;
    }

    @Override
    public Double getTotalIncomeByMonth(Long userId, int month, int year) {
        Double total = incomeRepository.sumAmountByUserIdAndMonth(userId, month, year);
        return total != null ? total : 0.0;
    }

    @Override
    public Double getTotalIncomeBySource(Long userId, String source) {
        Double total = incomeRepository.sumAmountByUserIdAndSource(userId, parseSource(source));
        return total != null ? total : 0.0;
    }

    // ─── Breakdown by Source ──────────────────────────────

    @Override
    public List<IncomeDto.IncomeBreakdownBySource> getBreakdownBySource(Long userId) {
        List<Income> allIncomes = incomeRepository.findByUserIdOrderByDateDesc(userId);
        double grandTotal = allIncomes.stream().mapToDouble(Income::getAmount).sum();

        // Group by source
        Map<Income.IncomeSource, DoubleSummaryStatistics> grouped = allIncomes.stream()
                .collect(Collectors.groupingBy(
                        Income::getSource,
                        Collectors.summarizingDouble(Income::getAmount)
                ));

        return grouped.entrySet().stream()
                .map(e -> {
                    IncomeDto.IncomeBreakdownBySource b = new IncomeDto.IncomeBreakdownBySource();
                    b.setSource(e.getKey().name());
                    b.setTotalAmount(e.getValue().getSum());
                    b.setCount(e.getValue().getCount());
                    b.setPercentage(grandTotal > 0
                            ? Math.round((e.getValue().getSum() / grandTotal * 100) * 100.0) / 100.0
                            : 0.0);
                    return b;
                })
                .sorted(Comparator.comparingDouble(IncomeDto.IncomeBreakdownBySource::getTotalAmount).reversed())
                .collect(Collectors.toList());
    }

    // ─── Helpers ──────────────────────────────────────────

    private Income findAndValidate(Long incomeId, Long userId) {
        Income income = incomeRepository.findByIncomeId(incomeId)
                .orElseThrow(() -> new RuntimeException("Income not found: " + incomeId));
        if (!income.getUserId().equals(userId)) {
            throw new SecurityException("Access denied: income does not belong to user");
        }
        return income;
    }

    private Income.IncomeSource parseSource(String source) {
        try {
            return source != null
                    ? Income.IncomeSource.valueOf(source.toUpperCase())
                    : Income.IncomeSource.OTHER;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid source: " + source +
                    ". Valid values: SALARY, FREELANCE, BUSINESS, INVESTMENT, GIFT, OTHER");
        }
    }

    private Income.RecurrencePeriod parseRecurrencePeriod(String period) {
        if (period == null) return null;
        try {
            return Income.RecurrencePeriod.valueOf(period.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid recurrencePeriod: " + period +
                    ". Valid values: DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY");
        }
    }

    private IncomeDto.IncomeResponse toResponse(Income income) {
        IncomeDto.IncomeResponse res = new IncomeDto.IncomeResponse();
        res.setIncomeId(income.getIncomeId());
        res.setUserId(income.getUserId());
        res.setCategoryId(income.getCategoryId());
        res.setTitle(income.getTitle());
        res.setAmount(income.getAmount());
        res.setCurrency(income.getCurrency());
        res.setSource(income.getSource().name());
        res.setDate(income.getDate());
        res.setNotes(income.getNotes());
        res.setIsRecurring(income.getIsRecurring());
        res.setRecurrencePeriod(income.getRecurrencePeriod() != null
                ? income.getRecurrencePeriod().name() : null);
        res.setCreatedAt(income.getCreatedAt() != null ? income.getCreatedAt().toString() : null);
        res.setUpdatedAt(income.getUpdatedAt() != null ? income.getUpdatedAt().toString() : null);
        return res;
    }
}
