package com.spendsmart.category.resource;

import com.spendsmart.category.dto.CategoryDto;
import com.spendsmart.category.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryResourceTest {

    @Mock
    private CategoryService categoryService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CategoryResource categoryResource;

    private CategoryDto.CategoryResponse categoryResponse;

    @BeforeEach
    void setUp() {
        categoryResponse = new CategoryDto.CategoryResponse();
        categoryResponse.setCategoryId(1L);
        categoryResponse.setName("Test Category");
    }

    @Test
    void create_ReturnsCreated() {
        when(authentication.getDetails()).thenReturn(1L);
        CategoryDto.CreateCategoryRequest req = new CategoryDto.CreateCategoryRequest();
        when(categoryService.createCategory(eq(1L), any())).thenReturn(categoryResponse);

        ResponseEntity<CategoryDto.CategoryResponse> res = categoryResource.create(authentication, req);

        assertEquals(HttpStatus.CREATED, res.getStatusCode());
        assertEquals(categoryResponse, res.getBody());
    }

    @Test
    void getById_ReturnsOk() {
        when(authentication.getDetails()).thenReturn(1L);
        when(categoryService.getCategoryById(1L, 1L)).thenReturn(categoryResponse);

        ResponseEntity<CategoryDto.CategoryResponse> res = categoryResource.getById(authentication, 1L);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(categoryResponse, res.getBody());
    }

    @Test
    void update_ReturnsOk() {
        when(authentication.getDetails()).thenReturn(1L);
        CategoryDto.UpdateCategoryRequest req = new CategoryDto.UpdateCategoryRequest();
        when(categoryService.updateCategory(eq(1L), eq(1L), any())).thenReturn(categoryResponse);

        ResponseEntity<CategoryDto.CategoryResponse> res = categoryResource.update(authentication, 1L, req);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(categoryResponse, res.getBody());
    }

    @Test
    void delete_ReturnsOk() {
        when(authentication.getDetails()).thenReturn(1L);
        CategoryDto.MessageResponse msg = new CategoryDto.MessageResponse("Deleted", true);
        when(categoryService.deleteCategory(1L, 1L)).thenReturn(msg);

        ResponseEntity<CategoryDto.MessageResponse> res = categoryResource.delete(authentication, 1L);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(msg, res.getBody());
    }

    @Test
    void getAll_ReturnsOk() {
        when(authentication.getDetails()).thenReturn(1L);
        when(categoryService.getAllForUser(1L)).thenReturn(List.of(categoryResponse));

        ResponseEntity<List<CategoryDto.CategoryResponse>> res = categoryResource.getAll(authentication);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(1, res.getBody().size());
    }

    @Test
    void getByType_ReturnsOk() {
        when(authentication.getDetails()).thenReturn(1L);
        when(categoryService.getAllForUserByType(1L, "EXPENSE")).thenReturn(List.of(categoryResponse));

        ResponseEntity<List<CategoryDto.CategoryResponse>> res = categoryResource.getByType(authentication, "EXPENSE");

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(1, res.getBody().size());
    }

    @Test
    void getCustom_ReturnsOk() {
        when(authentication.getDetails()).thenReturn(1L);
        when(categoryService.getCustomByUser(1L)).thenReturn(List.of(categoryResponse));

        ResponseEntity<List<CategoryDto.CategoryResponse>> res = categoryResource.getCustom(authentication);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(1, res.getBody().size());
    }

    @Test
    void getDefaults_ReturnsOk() {
        when(authentication.getDetails()).thenReturn(1L);
        when(categoryService.getDefaultCategories()).thenReturn(List.of(categoryResponse));

        ResponseEntity<List<CategoryDto.CategoryResponse>> res = categoryResource.getDefaults(authentication);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(1, res.getBody().size());
    }

    @Test
    void getDefaultsByType_ReturnsOk() {
        when(authentication.getDetails()).thenReturn(1L);
        when(categoryService.getDefaultsByType("EXPENSE")).thenReturn(List.of(categoryResponse));

        ResponseEntity<List<CategoryDto.CategoryResponse>> res = categoryResource.getDefaultsByType(authentication, "EXPENSE");

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(1, res.getBody().size());
    }

    @Test
    void setBudget_ReturnsOk() {
        when(authentication.getDetails()).thenReturn(1L);
        CategoryDto.SetBudgetLimitRequest req = new CategoryDto.SetBudgetLimitRequest();
        req.setBudgetLimit(500.0);
        when(categoryService.setCategoryBudgetLimit(1L, 1L, 500.0)).thenReturn(categoryResponse);

        ResponseEntity<CategoryDto.CategoryResponse> res = categoryResource.setBudget(authentication, 1L, req);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(categoryResponse, res.getBody());
    }

    @Test
    void getCount_ReturnsOk() {
        when(authentication.getDetails()).thenReturn(1L);
        when(categoryService.getCategoryCount(1L)).thenReturn(5L);

        ResponseEntity<CategoryDto.CountResponse> res = categoryResource.getCount(authentication);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(5L, res.getBody().getCount());
    }
}
