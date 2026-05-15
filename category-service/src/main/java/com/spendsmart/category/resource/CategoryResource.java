package com.spendsmart.category.resource;

import com.spendsmart.category.dto.CategoryDto;
import com.spendsmart.category.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryResource {

    private final CategoryService categoryService;

    // ─── CRUD ─────────────────────────────────────────────

    // POST /api/categories
    @PostMapping
    public ResponseEntity<CategoryDto.CategoryResponse> create(
            Authentication auth,
            @Valid @RequestBody CategoryDto.CreateCategoryRequest request) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryService.createCategory(userId, request));
    }

    // GET /api/categories/{id}
    @GetMapping("/{id}")
    public ResponseEntity<CategoryDto.CategoryResponse> getById(
            Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(categoryService.getCategoryById(id, userId));
    }

    // PUT /api/categories/{id}
    @PutMapping("/{id}")
    public ResponseEntity<CategoryDto.CategoryResponse> update(
            Authentication auth,
            @PathVariable Long id,
            @RequestBody CategoryDto.UpdateCategoryRequest request) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(categoryService.updateCategory(id, userId, request));
    }

    // DELETE /api/categories/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<CategoryDto.MessageResponse> delete(
            Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(categoryService.deleteCategory(id, userId));
    }

    // ─── List Queries ─────────────────────────────────────

    // GET /api/categories
    // Returns user's custom categories + all system defaults (merged)
    @GetMapping
    public ResponseEntity<List<CategoryDto.CategoryResponse>> getAll(Authentication auth) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(categoryService.getAllForUser(userId));
    }

    // GET /api/categories/type/{type}
    // type = EXPENSE | INCOME
    @GetMapping("/type/{type}")
    public ResponseEntity<List<CategoryDto.CategoryResponse>> getByType(
            Authentication auth, @PathVariable String type) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(categoryService.getAllForUserByType(userId, type));
    }

    // GET /api/categories/custom
    // Returns only the user's own custom categories (no defaults)
    @GetMapping("/custom")
    public ResponseEntity<List<CategoryDto.CategoryResponse>> getCustom(Authentication auth) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(categoryService.getCustomByUser(userId));
    }

    // GET /api/categories/defaults
    // Returns all system default categories (both EXPENSE and INCOME)
    @GetMapping("/defaults")
    public ResponseEntity<List<CategoryDto.CategoryResponse>> getDefaults(Authentication auth) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(categoryService.getDefaultCategories());
    }

    // GET /api/categories/defaults/{type}
    @GetMapping("/defaults/{type}")
    public ResponseEntity<List<CategoryDto.CategoryResponse>> getDefaultsByType(
            Authentication auth, @PathVariable String type) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(categoryService.getDefaultsByType(type));
    }

    // ─── Budget Limit ─────────────────────────────────────

    // PUT /api/categories/{id}/budget
    // Sets a monthly budget cap for the category
    @PutMapping("/{id}/budget")
    public ResponseEntity<CategoryDto.CategoryResponse> setBudget(
            Authentication auth,
            @PathVariable Long id,
            @RequestBody CategoryDto.SetBudgetLimitRequest request) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(
                categoryService.setCategoryBudgetLimit(id, userId, request.getBudgetLimit()));
    }

    // ─── Count ────────────────────────────────────────────

    // GET /api/categories/count
    @GetMapping("/count")
    public ResponseEntity<CategoryDto.CountResponse> getCount(Authentication auth) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(new CategoryDto.CountResponse(categoryService.getCategoryCount(userId)));
    }
}
