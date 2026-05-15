package com.spendsmart.category.serviceimpl;

import com.spendsmart.category.config.DefaultCategories;
import com.spendsmart.category.dto.CategoryDto;
import com.spendsmart.category.entity.Category;
import com.spendsmart.category.repository.CategoryRepository;
import com.spendsmart.category.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    // ─── CRUD ─────────────────────────────────────────────

    @Override
    @Transactional
    public CategoryDto.CategoryResponse createCategory(Long userId,
                                                        CategoryDto.CreateCategoryRequest request) {
        Category.CategoryType type = parseType(request.getType());

        // Prevent duplicate names within the same user + type
        if (categoryRepository.existsByUserIdAndNameAndType(userId, request.getName(), type)) {
            throw new IllegalArgumentException(
                    "Category '" + request.getName() + "' already exists for type " + type);
        }

        Category category = Category.builder()
                .userId(userId)
                .name(request.getName())
                .type(type)
                .icon(request.getIcon() != null ? request.getIcon() : "📦")
                .colorCode(request.getColorCode() != null ? request.getColorCode() : "#8b949e")
                .budgetLimit(request.getBudgetLimit())
                .isDefault(false)
                .build();

        return toResponse(categoryRepository.save(category));
    }

    @Override
    public CategoryDto.CategoryResponse getCategoryById(Long categoryId, Long userId) {
        Category cat = findAndValidateAccess(categoryId, userId);
        return toResponse(cat);
    }

    @Override
    @Transactional
    public CategoryDto.CategoryResponse updateCategory(Long categoryId, Long userId,
                                                        CategoryDto.UpdateCategoryRequest request) {
        Category cat = findAndValidateOwnership(categoryId, userId);

        if (request.getName() != null)      cat.setName(request.getName());
        if (request.getIcon() != null)      cat.setIcon(request.getIcon());
        if (request.getColorCode() != null) cat.setColorCode(request.getColorCode());

        return toResponse(categoryRepository.save(cat));
    }

    @Override
    @Transactional
    public CategoryDto.MessageResponse deleteCategory(Long categoryId, Long userId) {
        Category cat = findAndValidateOwnership(categoryId, userId);
        // Expenses/incomes linked to this category will fall back to
        // 'Uncategorised' in those services (handled client-side or via async event)
        categoryRepository.deleteByCategoryId(cat.getCategoryId());
        return new CategoryDto.MessageResponse(
                "Category deleted. Linked transactions moved to Uncategorised.", true);
    }

    // ─── Queries ──────────────────────────────────────────

    @Override
    public List<CategoryDto.CategoryResponse> getAllForUser(Long userId) {
        return categoryRepository.findAllForUser(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<CategoryDto.CategoryResponse> getAllForUserByType(Long userId, String type) {
        return categoryRepository.findAllForUserByType(userId, parseType(type))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<CategoryDto.CategoryResponse> getCustomByUser(Long userId) {
        return categoryRepository.findByUserId(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<CategoryDto.CategoryResponse> getDefaultCategories() {
        return categoryRepository.findByIsDefault(true)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<CategoryDto.CategoryResponse> getDefaultsByType(String type) {
        return categoryRepository.findByIsDefaultAndType(true, parseType(type))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ─── Budget Limit ─────────────────────────────────────

    @Override
    @Transactional
    public CategoryDto.CategoryResponse setCategoryBudgetLimit(Long categoryId, Long userId,
                                                                Double budgetLimit) {
        // Allow setting budget on default categories too (owned by platform),
        // so we only validate user can see it (not necessarily own it)
        Category cat = findAndValidateAccess(categoryId, userId);

        // If it's a default category, create a user-specific shadow copy
        if (Boolean.TRUE.equals(cat.getIsDefault())) {
            // Check if a custom copy already exists
            var existing = categoryRepository.findByUserIdAndName(userId, cat.getName());
            if (existing.isPresent()) {
                existing.get().setBudgetLimit(budgetLimit);
                return toResponse(categoryRepository.save(existing.get()));
            }
            // Create shadow copy of the default category for this user
            Category shadow = Category.builder()
                    .userId(userId)
                    .name(cat.getName())
                    .type(cat.getType())
                    .icon(cat.getIcon())
                    .colorCode(cat.getColorCode())
                    .budgetLimit(budgetLimit)
                    .isDefault(false)
                    .build();
            return toResponse(categoryRepository.save(shadow));
        }

        // Custom category — validate ownership then update
        findAndValidateOwnership(categoryId, userId);
        cat.setBudgetLimit(budgetLimit);
        return toResponse(categoryRepository.save(cat));
    }

    // ─── Seeding ──────────────────────────────────────────

    @Override
    @Transactional
    public void seedDefaultCategories() {
        int seeded = 0;

        for (DefaultCategories.DefaultCategoryDef def : DefaultCategories.EXPENSE_DEFAULTS) {
            if (!categoryRepository.existsByUserIdAndNameAndType(null, def.getName(), def.getType())) {
                categoryRepository.save(Category.builder()
                        .userId(null)
                        .name(def.getName())
                        .type(def.getType())
                        .icon(def.getIcon())
                        .colorCode(def.getColorCode())
                        .isDefault(true)
                        .build());
                seeded++;
            }
        }

        for (DefaultCategories.DefaultCategoryDef def : DefaultCategories.INCOME_DEFAULTS) {
            if (!categoryRepository.existsByUserIdAndNameAndType(null, def.getName(), def.getType())) {
                categoryRepository.save(Category.builder()
                        .userId(null)
                        .name(def.getName())
                        .type(def.getType())
                        .icon(def.getIcon())
                        .colorCode(def.getColorCode())
                        .isDefault(true)
                        .build());
                seeded++;
            }
        }

        if (seeded > 0) {
            log.info("CategoryService: seeded {} default categories", seeded);
        } else {
            log.info("CategoryService: default categories already present, skipping seed");
        }
    }

    // ─── Count ────────────────────────────────────────────

    @Override
    public long getCategoryCount(Long userId) {
        return categoryRepository.countByUserId(userId);
    }

    // ─── Helpers ──────────────────────────────────────────

    /**
     * Validates access: user can read any default category OR their own custom category.
     */
    private Category findAndValidateAccess(Long categoryId, Long userId) {
        Category cat = categoryRepository.findByCategoryId(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found: " + categoryId));

        boolean isDefault = Boolean.TRUE.equals(cat.getIsDefault());
        boolean isOwner   = userId.equals(cat.getUserId());

        if (!isDefault && !isOwner) {
            throw new SecurityException("Access denied: category does not belong to user");
        }
        return cat;
    }

    /**
     * Stricter than findAndValidateAccess — user must OWN the category (not just see it).
     * Default categories cannot be edited or deleted.
     */
    private Category findAndValidateOwnership(Long categoryId, Long userId) {
        Category cat = categoryRepository.findByCategoryId(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found: " + categoryId));

        if (Boolean.TRUE.equals(cat.getIsDefault())) {
            throw new IllegalArgumentException("Default categories cannot be modified or deleted");
        }
        if (!userId.equals(cat.getUserId())) {
            throw new SecurityException("Access denied: category does not belong to user");
        }
        return cat;
    }

    private Category.CategoryType parseType(String type) {
        try {
            return Category.CategoryType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException(
                    "Invalid category type: " + type + ". Must be EXPENSE or INCOME");
        }
    }

    private CategoryDto.CategoryResponse toResponse(Category cat) {
        CategoryDto.CategoryResponse res = new CategoryDto.CategoryResponse();
        res.setCategoryId(cat.getCategoryId());
        res.setUserId(cat.getUserId());
        res.setName(cat.getName());
        res.setType(cat.getType().name());
        res.setIcon(cat.getIcon());
        res.setColorCode(cat.getColorCode());
        res.setBudgetLimit(cat.getBudgetLimit());
        res.setIsDefault(cat.getIsDefault());
        res.setCreatedAt(cat.getCreatedAt() != null ? cat.getCreatedAt().toString() : null);
        return res;
    }
}
