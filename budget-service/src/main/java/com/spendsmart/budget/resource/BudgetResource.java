package com.spendsmart.budget.resource;

import com.spendsmart.budget.dto.BudgetDto;
import com.spendsmart.budget.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetResource {

    private final BudgetService budgetService;

    @PostMapping
    public ResponseEntity<BudgetDto.BudgetResponse> create(Authentication auth, @Valid @RequestBody BudgetDto.CreateBudgetRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(budgetService.createBudget((Long) auth.getDetails(), req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BudgetDto.BudgetResponse> getById(Authentication auth, @PathVariable Long id) {
        return ResponseEntity.ok(budgetService.getBudgetById(id, (Long) auth.getDetails()));
    }

    @GetMapping
    public ResponseEntity<List<BudgetDto.BudgetResponse>> getAll(Authentication auth) {
        return ResponseEntity.ok(budgetService.getBudgetsByUser((Long) auth.getDetails()));
    }

    @GetMapping("/active")
    public ResponseEntity<List<BudgetDto.BudgetResponse>> getActive(Authentication auth) {
        return ResponseEntity.ok(budgetService.getActiveBudgets((Long) auth.getDetails()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BudgetDto.BudgetResponse> update(Authentication auth, @PathVariable Long id, @RequestBody BudgetDto.UpdateBudgetRequest req) {
        return ResponseEntity.ok(budgetService.updateBudget(id, (Long) auth.getDetails(), req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BudgetDto.MessageResponse> delete(Authentication auth, @PathVariable Long id) {
        return ResponseEntity.ok(budgetService.deleteBudget(id, (Long) auth.getDetails()));
    }

    // Called by expense-service when a transaction changes
    @PutMapping("/{id}/spent")
    public ResponseEntity<BudgetDto.BudgetResponse> updateSpent(Authentication auth, @PathVariable Long id, @RequestBody BudgetDto.UpdateSpentRequest req) {
        return ResponseEntity.ok(budgetService.updateSpentAmount(id, (Long) auth.getDetails(), req.getDelta()));
    }

    @GetMapping("/{id}/progress")
    public ResponseEntity<BudgetDto.BudgetProgress> getProgress(Authentication auth, @PathVariable Long id) {
        return ResponseEntity.ok(budgetService.getBudgetProgress(id, (Long) auth.getDetails()));
    }

    @GetMapping("/progress")
    public ResponseEntity<List<BudgetDto.BudgetProgress>> getAllProgress(Authentication auth) {
        return ResponseEntity.ok(budgetService.getAllProgress((Long) auth.getDetails()));
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<String>> getAlerts(Authentication auth) {
        return ResponseEntity.ok(budgetService.checkBudgetAlerts((Long) auth.getDetails()));
    }

    @PostMapping("/reset")
    public ResponseEntity<BudgetDto.MessageResponse> resetPeriod(Authentication auth, @RequestParam String period) {
        budgetService.resetBudgetPeriods((Long) auth.getDetails(), period);
        return ResponseEntity.ok(new BudgetDto.MessageResponse("Budget periods reset for " + period, true));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<BudgetDto.BudgetResponse>> getByCategory(Authentication auth, @PathVariable Long categoryId) {
        return ResponseEntity.ok(budgetService.getBudgetsByCategory((Long) auth.getDetails(), categoryId));
    }
}
