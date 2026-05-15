package com.spendsmart.income.resource;

import com.spendsmart.income.dto.IncomeDto;
import com.spendsmart.income.service.IncomeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/incomes")
@RequiredArgsConstructor
public class IncomeResource {

    private final IncomeService incomeService;

    // ─── CRUD ─────────────────────────────────────────────

    // POST /api/incomes
    @PostMapping
    public ResponseEntity<IncomeDto.IncomeResponse> add(
            Authentication auth,
            @Valid @RequestBody IncomeDto.CreateIncomeRequest request) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(incomeService.addIncome(userId, request));
    }

    // GET /api/incomes/{id}
    @GetMapping("/{id}")
    public ResponseEntity<IncomeDto.IncomeResponse> getById(
            Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(incomeService.getIncomeById(id, userId));
    }

    // PUT /api/incomes/{id}
    @PutMapping("/{id}")
    public ResponseEntity<IncomeDto.IncomeResponse> update(
            Authentication auth,
            @PathVariable Long id,
            @RequestBody IncomeDto.UpdateIncomeRequest request) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(incomeService.updateIncome(id, userId, request));
    }

    // DELETE /api/incomes/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<IncomeDto.MessageResponse> delete(
            Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(incomeService.deleteIncome(id, userId));
    }

    // ─── List Queries ─────────────────────────────────────

    // GET /api/incomes
    @GetMapping
    public ResponseEntity<List<IncomeDto.IncomeResponse>> getAll(Authentication auth) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(incomeService.getIncomesByUser(userId));
    }

    // GET /api/incomes/source/{source}
    // source = SALARY | FREELANCE | BUSINESS | INVESTMENT | GIFT | OTHER
    @GetMapping("/source/{source}")
    public ResponseEntity<List<IncomeDto.IncomeResponse>> getBySource(
            Authentication auth, @PathVariable String source) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(incomeService.getIncomesBySource(userId, source));
    }

    // GET /api/incomes/date-range?start=2026-01-01&end=2026-01-31
    @GetMapping("/date-range")
    public ResponseEntity<List<IncomeDto.IncomeResponse>> getByDateRange(
            Authentication auth,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(incomeService.getIncomesByDateRange(userId, start, end));
    }

    // GET /api/incomes/month?month=4&year=2026
    @GetMapping("/month")
    public ResponseEntity<List<IncomeDto.IncomeResponse>> getByMonth(
            Authentication auth,
            @RequestParam int month,
            @RequestParam int year) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(incomeService.getIncomesByMonth(userId, month, year));
    }

    // GET /api/incomes/category/{categoryId}
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<IncomeDto.IncomeResponse>> getByCategory(
            Authentication auth, @PathVariable Long categoryId) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(incomeService.getIncomesByCategory(userId, categoryId));
    }

    // GET /api/incomes/search?keyword=salary
    @GetMapping("/search")
    public ResponseEntity<List<IncomeDto.IncomeResponse>> search(
            Authentication auth, @RequestParam String keyword) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(incomeService.searchIncomes(userId, keyword));
    }

    // ─── Recurring ────────────────────────────────────────

    // GET /api/incomes/recurring
    @GetMapping("/recurring")
    public ResponseEntity<List<IncomeDto.IncomeResponse>> getRecurring(Authentication auth) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(incomeService.getRecurringIncomes(userId));
    }

    // ─── Aggregations ─────────────────────────────────────

    // GET /api/incomes/total
    @GetMapping("/total")
    public ResponseEntity<Map<String, Double>> getTotal(Authentication auth) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(Map.of("total", incomeService.getTotalIncomeByUser(userId)));
    }

    // GET /api/incomes/total/month?month=4&year=2026
    @GetMapping("/total/month")
    public ResponseEntity<Map<String, Double>> getTotalByMonth(
            Authentication auth,
            @RequestParam int month,
            @RequestParam int year) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(Map.of("total", incomeService.getTotalIncomeByMonth(userId, month, year)));
    }

    // GET /api/incomes/total/source/{source}
    @GetMapping("/total/source/{source}")
    public ResponseEntity<Map<String, Double>> getTotalBySource(
            Authentication auth, @PathVariable String source) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(Map.of("total", incomeService.getTotalIncomeBySource(userId, source)));
    }

    // ─── Breakdown ────────────────────────────────────────

    // GET /api/incomes/breakdown/source
    // Returns % share of each income source — used by Analytics pie chart
    @GetMapping("/breakdown/source")
    public ResponseEntity<List<IncomeDto.IncomeBreakdownBySource>> getSourceBreakdown(Authentication auth) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(incomeService.getBreakdownBySource(userId));
    }
}
