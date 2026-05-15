package com.spendsmart.budget.serviceimpl;

import com.spendsmart.budget.dto.BudgetDto;
import com.spendsmart.budget.dto.NotificationMessage;
import com.spendsmart.budget.entity.Budget;
import com.spendsmart.budget.repository.BudgetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceImplTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private BudgetServiceImpl budgetService;

    private Budget budget;

    @BeforeEach
    void setUp() {
        budget = Budget.builder()
                .budgetId(1L)
                .userId(1L)
                .categoryId(100L)
                .name("Groceries")
                .limitAmount(1000.0)
                .spentAmount(0.0)
                .currency("INR")
                .period(Budget.BudgetPeriod.MONTHLY)
                .startDate(LocalDate.now().withDayOfMonth(1))
                .endDate(LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()))
                .alertThreshold(80)
                .isActive(true)
                .build();
    }

    @Test
    void createBudget_Success() {
        BudgetDto.CreateBudgetRequest req = new BudgetDto.CreateBudgetRequest();
        req.setCategoryId(100L);
        req.setName("Groceries");
        req.setLimitAmount(1000.0);
        req.setCurrency("INR");
        req.setPeriod("MONTHLY");

        when(budgetRepository.save(any(Budget.class))).thenReturn(budget);

        BudgetDto.BudgetResponse res = budgetService.createBudget(1L, req);

        assertNotNull(res);
        assertEquals("Groceries", res.getName());
        assertEquals(1000.0, res.getLimitAmount());
        verify(budgetRepository, times(1)).save(any(Budget.class));
    }

    @Test
    void getBudgetById_Success() {
        when(budgetRepository.findByBudgetId(1L)).thenReturn(Optional.of(budget));
        BudgetDto.BudgetResponse res = budgetService.getBudgetById(1L, 1L);
        assertNotNull(res);
        assertEquals("Groceries", res.getName());
    }


    @Test
    void getBudgetsByUser_Success() {
        when(budgetRepository.findByUserId(1L)).thenReturn(List.of(budget));
        List<BudgetDto.BudgetResponse> res = budgetService.getBudgetsByUser(1L);
        assertEquals(1, res.size());
    }

    @Test
    void getActiveBudgets_Success() {
        when(budgetRepository.findByUserIdAndIsActive(1L, true)).thenReturn(List.of(budget));
        List<BudgetDto.BudgetResponse> res = budgetService.getActiveBudgets(1L);
        assertEquals(1, res.size());
    }

    @Test
    void updateBudget_Success() {
        BudgetDto.UpdateBudgetRequest req = new BudgetDto.UpdateBudgetRequest();
        req.setName("Updated Groceries");
        req.setLimitAmount(1200.0);

        when(budgetRepository.findByBudgetId(1L)).thenReturn(Optional.of(budget));
        when(budgetRepository.save(any(Budget.class))).thenReturn(budget);

        BudgetDto.BudgetResponse res = budgetService.updateBudget(1L, 1L, req);

        assertEquals("Updated Groceries", budget.getName());
        assertEquals(1200.0, budget.getLimitAmount());
    }

    @Test
    void deleteBudget_Success() {
        when(budgetRepository.findByBudgetId(1L)).thenReturn(Optional.of(budget));
        BudgetDto.MessageResponse res = budgetService.deleteBudget(1L, 1L);
        assertTrue(res.isSuccess());
        verify(budgetRepository, times(1)).deleteByBudgetId(1L);
    }

    @Test
    void updateSpentAmount_NoAlert() {
        when(budgetRepository.findByBudgetId(1L)).thenReturn(Optional.of(budget));
        when(budgetRepository.save(any(Budget.class))).thenReturn(budget);

        BudgetDto.BudgetResponse res = budgetService.updateSpentAmount(1L, 1L, 500.0);

        assertEquals(500.0, budget.getSpentAmount());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(NotificationMessage.class));
    }

    @Test
    void updateSpentAmount_TriggersAlert() {
        when(budgetRepository.findByBudgetId(1L)).thenReturn(Optional.of(budget));
        when(budgetRepository.save(any(Budget.class))).thenReturn(budget);

        BudgetDto.BudgetResponse res = budgetService.updateSpentAmount(1L, 1L, 850.0);

        assertEquals(850.0, budget.getSpentAmount());
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(NotificationMessage.class));
    }

    @Test
    void updateSpentAmount_TriggersExceeded() {
        budget.setSpentAmount(900.0); // Already warned
        when(budgetRepository.findByBudgetId(1L)).thenReturn(Optional.of(budget));
        when(budgetRepository.save(any(Budget.class))).thenReturn(budget);

        BudgetDto.BudgetResponse res = budgetService.updateSpentAmount(1L, 1L, 150.0);

        assertEquals(1050.0, budget.getSpentAmount());
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(NotificationMessage.class));
    }


    @Test
    void getBudgetProgress_Success() {
        budget.setSpentAmount(500.0);
        when(budgetRepository.findByBudgetId(1L)).thenReturn(Optional.of(budget));
        
        BudgetDto.BudgetProgress progress = budgetService.getBudgetProgress(1L, 1L);
        
        assertEquals(50.0, progress.getPercentageUsed());
        assertEquals("SAFE", progress.getStatus());
        assertEquals(500.0, progress.getRemainingAmount());
    }

    @Test
    void getAllProgress_Success() {
        when(budgetRepository.findByUserIdAndIsActive(1L, true)).thenReturn(List.of(budget));
        List<BudgetDto.BudgetProgress> res = budgetService.getAllProgress(1L);
        assertEquals(1, res.size());
    }

    @Test
    void checkBudgetAlerts_ReturnsAlerts() {
        budget.setSpentAmount(900.0); // 90%
        when(budgetRepository.findByUserIdAndIsActive(1L, true)).thenReturn(List.of(budget));
        
        List<String> alerts = budgetService.checkBudgetAlerts(1L);
        
        assertEquals(1, alerts.size());
        assertTrue(alerts.get(0).contains("90%"));
    }

    @Test
    void resetBudgetPeriods_Success() {
        budgetService.resetBudgetPeriods(1L, "MONTHLY");
        verify(budgetRepository, times(1)).resetPeriodForUser(1L, Budget.BudgetPeriod.MONTHLY);
    }

    @Test
    void getBudgetsByCategory_Success() {
        when(budgetRepository.findByUserIdAndCategoryId(1L, 100L)).thenReturn(List.of(budget));
        List<BudgetDto.BudgetResponse> res = budgetService.getBudgetsByCategory(1L, 100L);
        assertEquals(1, res.size());
    }

}
