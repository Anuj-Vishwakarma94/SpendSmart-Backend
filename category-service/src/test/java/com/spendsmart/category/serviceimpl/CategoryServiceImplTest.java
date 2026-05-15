package com.spendsmart.category.serviceimpl;

import com.spendsmart.category.dto.CategoryDto;
import com.spendsmart.category.entity.Category;
import com.spendsmart.category.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category customCategory;
    private Category defaultCategory;

    @BeforeEach
    void setUp() {
        customCategory = Category.builder()
                .categoryId(1L)
                .userId(1L)
                .name("Custom Food")
                .type(Category.CategoryType.EXPENSE)
                .icon("🍔")
                .colorCode("#FF0000")
                .isDefault(false)
                .build();

        defaultCategory = Category.builder()
                .categoryId(2L)
                .userId(null)
                .name("Default Rent")
                .type(Category.CategoryType.EXPENSE)
                .icon("🏠")
                .colorCode("#00FF00")
                .isDefault(true)
                .build();
    }

    @Test
    void createCategory_Success() {
        CategoryDto.CreateCategoryRequest req = new CategoryDto.CreateCategoryRequest();
        req.setName("Custom Food");
        req.setType("EXPENSE");

        when(categoryRepository.existsByUserIdAndNameAndType(1L, "Custom Food", Category.CategoryType.EXPENSE))
                .thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(customCategory);

        CategoryDto.CategoryResponse res = categoryService.createCategory(1L, req);

        assertNotNull(res);
        assertEquals("Custom Food", res.getName());
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    void createCategory_Duplicate_ThrowsException() {
        CategoryDto.CreateCategoryRequest req = new CategoryDto.CreateCategoryRequest();
        req.setName("Custom Food");
        req.setType("EXPENSE");

        when(categoryRepository.existsByUserIdAndNameAndType(1L, "Custom Food", Category.CategoryType.EXPENSE))
                .thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> categoryService.createCategory(1L, req));
    }

    @Test
    void getCategoryById_Custom_Success() {
        when(categoryRepository.findByCategoryId(1L)).thenReturn(Optional.of(customCategory));
        CategoryDto.CategoryResponse res = categoryService.getCategoryById(1L, 1L);
        assertNotNull(res);
        assertEquals("Custom Food", res.getName());
    }

    @Test
    void getCategoryById_Default_Success() {
        when(categoryRepository.findByCategoryId(2L)).thenReturn(Optional.of(defaultCategory));
        CategoryDto.CategoryResponse res = categoryService.getCategoryById(2L, 1L); // User 1 accesses default
        assertNotNull(res);
        assertEquals("Default Rent", res.getName());
    }


    @Test
    void updateCategory_Success() {
        CategoryDto.UpdateCategoryRequest req = new CategoryDto.UpdateCategoryRequest();
        req.setName("Updated Food");

        when(categoryRepository.findByCategoryId(1L)).thenReturn(Optional.of(customCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(customCategory);

        CategoryDto.CategoryResponse res = categoryService.updateCategory(1L, 1L, req);

        assertEquals("Updated Food", customCategory.getName());
    }


    @Test
    void deleteCategory_Success() {
        when(categoryRepository.findByCategoryId(1L)).thenReturn(Optional.of(customCategory));
        CategoryDto.MessageResponse res = categoryService.deleteCategory(1L, 1L);
        assertTrue(res.isSuccess());
        verify(categoryRepository, times(1)).deleteByCategoryId(1L);
    }


    @Test
    void getAllForUser_Success() {
        when(categoryRepository.findAllForUser(1L)).thenReturn(List.of(customCategory, defaultCategory));
        List<CategoryDto.CategoryResponse> res = categoryService.getAllForUser(1L);
        assertEquals(2, res.size());
    }

    @Test
    void getAllForUserByType_Success() {
        when(categoryRepository.findAllForUserByType(1L, Category.CategoryType.EXPENSE))
                .thenReturn(List.of(customCategory));
        List<CategoryDto.CategoryResponse> res = categoryService.getAllForUserByType(1L, "EXPENSE");
        assertEquals(1, res.size());
    }

    @Test
    void getCustomByUser_Success() {
        when(categoryRepository.findByUserId(1L)).thenReturn(List.of(customCategory));
        List<CategoryDto.CategoryResponse> res = categoryService.getCustomByUser(1L);
        assertEquals(1, res.size());
    }

    @Test
    void getDefaultCategories_Success() {
        when(categoryRepository.findByIsDefault(true)).thenReturn(List.of(defaultCategory));
        List<CategoryDto.CategoryResponse> res = categoryService.getDefaultCategories();
        assertEquals(1, res.size());
    }

    @Test
    void getDefaultsByType_Success() {
        when(categoryRepository.findByIsDefaultAndType(true, Category.CategoryType.EXPENSE))
                .thenReturn(List.of(defaultCategory));
        List<CategoryDto.CategoryResponse> res = categoryService.getDefaultsByType("EXPENSE");
        assertEquals(1, res.size());
    }

    @Test
    void setCategoryBudgetLimit_Custom_Success() {
        when(categoryRepository.findByCategoryId(1L)).thenReturn(Optional.of(customCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(customCategory);

        CategoryDto.CategoryResponse res = categoryService.setCategoryBudgetLimit(1L, 1L, 500.0);
        assertEquals(500.0, customCategory.getBudgetLimit());
    }

    @Test
    void setCategoryBudgetLimit_Default_CreatesShadowCopy() {
        when(categoryRepository.findByCategoryId(2L)).thenReturn(Optional.of(defaultCategory));
        when(categoryRepository.findByUserIdAndName(1L, "Default Rent")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        CategoryDto.CategoryResponse res = categoryService.setCategoryBudgetLimit(2L, 1L, 500.0);
        
        assertEquals(1L, res.getUserId());
        assertEquals("Default Rent", res.getName());
        assertEquals(500.0, res.getBudgetLimit());
        assertFalse(res.getIsDefault());
    }


    @Test
    void seedDefaultCategories_RunsSuccessfully() {
        when(categoryRepository.existsByUserIdAndNameAndType(any(), any(), any())).thenReturn(false);
        categoryService.seedDefaultCategories();
        verify(categoryRepository, atLeastOnce()).save(any(Category.class));
    }

    @Test
    void seedDefaultCategories_AlreadySeeded() {
        when(categoryRepository.existsByUserIdAndNameAndType(any(), any(), any())).thenReturn(true);
        categoryService.seedDefaultCategories();
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void getCategoryCount_Success() {
        when(categoryRepository.countByUserId(1L)).thenReturn(5L);
        long count = categoryService.getCategoryCount(1L);
        assertEquals(5L, count);
    }

}
