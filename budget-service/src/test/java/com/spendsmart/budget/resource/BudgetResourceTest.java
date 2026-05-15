package com.spendsmart.budget.resource;

import com.spendsmart.budget.dto.BudgetDto;
import com.spendsmart.budget.service.BudgetService;
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
class BudgetResourceTest {

    @Mock
    private BudgetService budgetService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private BudgetResource budgetResource;

    private BudgetDto.BudgetResponse budgetResponse;
    private BudgetDto.BudgetProgress budgetProgress;

    @BeforeEach
    void setUp() {
        budgetResponse = new BudgetDto.BudgetResponse();
        budgetResponse.setBudgetId(1L);
        budgetResponse.setName("Test Budget");

        budgetProgress = new BudgetDto.BudgetProgress();
        budgetProgress.setBudgetId(1L);
        budgetProgress.setPercentageUsed(50.0);
    }

    @Test
    void create_ReturnsCreated() {
        when(authentication.getDetails()).thenReturn(1L);
        BudgetDto.CreateBudgetRequest req = new BudgetDto.CreateBudgetRequest();
        when(budgetService.createBudget(eq(1L), any())).thenReturn(budgetResponse);

        ResponseEntity<BudgetDto.BudgetResponse> res = budgetResource.create(authentication, req);

        assertEquals(HttpStatus.CREATED, res.getStatusCode());
        assertEquals(budgetResponse, res.getBody());
    }

    @Test
    void getById_ReturnsOk() {
        when(authentication.getDetails()).thenReturn(1L);
        when(budgetService.getBudgetById(1L, 1L)).thenReturn(budgetResponse);

        ResponseEntity<BudgetDto.BudgetResponse> res = budgetResource.getById(authentication, 1L);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(budgetResponse, res.getBody());
    }

    @Test
    void getAll_ReturnsOk() {
        when(authentication.getDetails()).thenReturn(1L);
        when(budgetService.getBudgetsByUser(1L)).thenReturn(List.of(budgetResponse));

        ResponseEntity<List<BudgetDto.BudgetResponse>> res = budgetResource.getAll(authentication);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(1, res.getBody().size());
    }

    @Test
    void getActive_ReturnsOk() {
        when(authentication.getDetails()).thenReturn(1L);
        when(budgetService.getActiveBudgets(1L)).thenReturn(List.of(budgetResponse));

        ResponseEntity<List<BudgetDto.BudgetResponse>> res = budgetResource.getActive(authentication);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(1, res.getBody().size());
    }

    @Test
    void update_ReturnsOk() {
        when(authentication.getDetails()).thenReturn(1L);
        BudgetDto.UpdateBudgetRequest req = new BudgetDto.UpdateBudgetRequest();
        when(budgetService.updateBudget(eq(1L), eq(1L), any())).thenReturn(budgetResponse);

        ResponseEntity<BudgetDto.BudgetResponse> res = budgetResource.update(authentication, 1L, req);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(budgetResponse, res.getBody());
    }

    @Test
    void delete_ReturnsOk() {
        when(authentication.getDetails()).thenReturn(1L);
        BudgetDto.MessageResponse msg = new BudgetDto.MessageResponse("Deleted", true);
        when(budgetService.deleteBudget(1L, 1L)).thenReturn(msg);

        ResponseEntity<BudgetDto.MessageResponse> res = budgetResource.delete(authentication, 1L);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(msg, res.getBody());
    }

    @Test
    void updateSpent_ReturnsOk() {
        when(authentication.getDetails()).thenReturn(1L);
        BudgetDto.UpdateSpentRequest req = new BudgetDto.UpdateSpentRequest();
        req.setDelta(50.0);
        when(budgetService.updateSpentAmount(1L, 1L, 50.0)).thenReturn(budgetResponse);

        ResponseEntity<BudgetDto.BudgetResponse> res = budgetResource.updateSpent(authentication, 1L, req);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(budgetResponse, res.getBody());
    }

    @Test
    void getProgress_ReturnsOk() {
        when(authentication.getDetails()).thenReturn(1L);
        when(budgetService.getBudgetProgress(1L, 1L)).thenReturn(budgetProgress);

        ResponseEntity<BudgetDto.BudgetProgress> res = budgetResource.getProgress(authentication, 1L);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(budgetProgress, res.getBody());
    }

    @Test
    void getAllProgress_ReturnsOk() {
        when(authentication.getDetails()).thenReturn(1L);
        when(budgetService.getAllProgress(1L)).thenReturn(List.of(budgetProgress));

        ResponseEntity<List<BudgetDto.BudgetProgress>> res = budgetResource.getAllProgress(authentication);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(1, res.getBody().size());
    }

    @Test
    void getAlerts_ReturnsOk() {
        when(authentication.getDetails()).thenReturn(1L);
        when(budgetService.checkBudgetAlerts(1L)).thenReturn(List.of("Alert!"));

        ResponseEntity<List<String>> res = budgetResource.getAlerts(authentication);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(1, res.getBody().size());
    }

    @Test
    void resetPeriod_ReturnsOk() {
        when(authentication.getDetails()).thenReturn(1L);
        doNothing().when(budgetService).resetBudgetPeriods(1L, "MONTHLY");

        ResponseEntity<BudgetDto.MessageResponse> res = budgetResource.resetPeriod(authentication, "MONTHLY");

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals("Budget periods reset for MONTHLY", res.getBody().getMessage());
    }

    @Test
    void getByCategory_ReturnsOk() {
        when(authentication.getDetails()).thenReturn(1L);
        when(budgetService.getBudgetsByCategory(1L, 100L)).thenReturn(List.of(budgetResponse));

        ResponseEntity<List<BudgetDto.BudgetResponse>> res = budgetResource.getByCategory(authentication, 100L);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(1, res.getBody().size());
    }
}
