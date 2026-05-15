package com.spendsmart.expense.resource;

import com.spendsmart.expense.dto.ExpenseDto;
import com.spendsmart.expense.service.ExpenseService;
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
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseResource {

    private final ExpenseService expenseService;

    // POST /api/expenses
    @PostMapping
    public ResponseEntity<ExpenseDto.ExpenseResponse> addExpense(
            Authentication auth,
            @Valid @RequestBody ExpenseDto.CreateExpenseRequest request) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.status(HttpStatus.CREATED).body(expenseService.addExpense(userId, request));
    }

    // GET /api/expenses/admin/all  — ADMIN only, all platform expenses
    @GetMapping("/admin/all")
    public ResponseEntity<List<ExpenseDto.ExpenseResponse>> getAllAdmin() {
        return ResponseEntity.ok(expenseService.getAllExpenses());
    }

    // GET /api/expenses/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ExpenseDto.ExpenseResponse> getById(
            Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(expenseService.getExpenseById(id, userId));
    }

    // GET /api/expenses
    @GetMapping
    public ResponseEntity<List<ExpenseDto.ExpenseResponse>> getAll(Authentication auth) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(expenseService.getExpensesByUser(userId));
    }

    // GET /api/expenses/category/{categoryId}
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ExpenseDto.ExpenseResponse>> getByCategory(
            Authentication auth, @PathVariable Long categoryId) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(expenseService.getExpensesByCategory(userId, categoryId));
    }

    // GET /api/expenses/date-range?start=2026-01-01&end=2026-01-31
    @GetMapping("/date-range")
    public ResponseEntity<List<ExpenseDto.ExpenseResponse>> getByDateRange(
            Authentication auth,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(expenseService.getExpensesByDateRange(userId, start, end));
    }

    // GET /api/expenses/month?month=4&year=2026
    @GetMapping("/month")
    public ResponseEntity<List<ExpenseDto.ExpenseResponse>> getByMonth(
            Authentication auth,
            @RequestParam int month,
            @RequestParam int year) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(expenseService.getExpensesByMonth(userId, month, year));
    }

    // GET /api/expenses/type/{type}
    @GetMapping("/type/{type}")
    public ResponseEntity<List<ExpenseDto.ExpenseResponse>> getByType(
            Authentication auth, @PathVariable String type) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(expenseService.getExpensesByType(userId, type));
    }

    // GET /api/expenses/search?keyword=food
    @GetMapping("/search")
    public ResponseEntity<List<ExpenseDto.ExpenseResponse>> search(
            Authentication auth, @RequestParam String keyword) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(expenseService.searchExpenses(userId, keyword));
    }

    // GET /api/expenses/amount-range?min=100&max=5000
    @GetMapping("/amount-range")
    public ResponseEntity<List<ExpenseDto.ExpenseResponse>> getByAmountRange(
            Authentication auth,
            @RequestParam Double min,
            @RequestParam Double max) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(expenseService.getExpensesByAmountRange(userId, min, max));
    }

    // PUT /api/expenses/{id}
    @PutMapping("/{id}")
    public ResponseEntity<ExpenseDto.ExpenseResponse> update(
            Authentication auth,
            @PathVariable Long id,
            @RequestBody ExpenseDto.UpdateExpenseRequest request) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(expenseService.updateExpense(id, userId, request));
    }

    // DELETE /api/expenses/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<ExpenseDto.MessageResponse> delete(
            Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(expenseService.deleteExpense(id, userId));
    }

    // GET /api/expenses/total
    @GetMapping("/total")
    public ResponseEntity<Map<String, Double>> getTotal(Authentication auth) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(Map.of("total", expenseService.getTotalByUser(userId)));
    }

    // GET /api/expenses/total/category/{categoryId}
    @GetMapping("/total/category/{categoryId}")
    public ResponseEntity<Map<String, Double>> getTotalByCategory(
            Authentication auth, @PathVariable Long categoryId) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(Map.of("total", expenseService.getTotalByCategory(userId, categoryId)));
    }

    // GET /api/expenses/total/month?month=4&year=2026
    @GetMapping("/total/month")
    public ResponseEntity<Map<String, Double>> getTotalByMonth(
            Authentication auth,
            @RequestParam int month,
            @RequestParam int year) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(Map.of("total", expenseService.getTotalByMonth(userId, month, year)));
    }
}
