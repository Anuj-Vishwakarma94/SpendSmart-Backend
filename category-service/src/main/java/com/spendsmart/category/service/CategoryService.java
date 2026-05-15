package com.spendsmart.category.service;

import com.spendsmart.category.dto.CategoryDto;

import java.util.List;

public interface CategoryService {

    // ─── CRUD ─────────────────────────────────────────────
    CategoryDto.CategoryResponse createCategory(Long userId, CategoryDto.CreateCategoryRequest request);

    CategoryDto.CategoryResponse getCategoryById(Long categoryId, Long userId);

    CategoryDto.CategoryResponse updateCategory(Long categoryId, Long userId, CategoryDto.UpdateCategoryRequest request);

    CategoryDto.MessageResponse deleteCategory(Long categoryId, Long userId);

    // ─── Queries ──────────────────────────────────────────

    /** Returns user's own categories + system defaults (merged, deduped). */
    List<CategoryDto.CategoryResponse> getAllForUser(Long userId);

    /** Returns user's own categories + system defaults filtered by type. */
    List<CategoryDto.CategoryResponse> getAllForUserByType(Long userId, String type);

    /** Returns only the user's custom (non-default) categories. */
    List<CategoryDto.CategoryResponse> getCustomByUser(Long userId);

    /** Returns only the system default categories. */
    List<CategoryDto.CategoryResponse> getDefaultCategories();

    /** Returns system defaults filtered by type. */
    List<CategoryDto.CategoryResponse> getDefaultsByType(String type);

    // ─── Budget Limit ─────────────────────────────────────
    CategoryDto.CategoryResponse setCategoryBudgetLimit(Long categoryId, Long userId, Double budgetLimit);

    // ─── Seeding ──────────────────────────────────────────

    /**
     * Called at application startup via ApplicationRunner.
     * Inserts all default categories if they don't already exist.
     */
    void seedDefaultCategories();

    // ─── Count ────────────────────────────────────────────
    long getCategoryCount(Long userId);
}
