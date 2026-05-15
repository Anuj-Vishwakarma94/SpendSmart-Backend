package com.spendsmart.expense.serviceimpl;

import com.spendsmart.expense.client.BudgetServiceClient;
import com.spendsmart.expense.dto.ExpenseDto;
import com.spendsmart.expense.entity.Expense;
import com.spendsmart.expense.repository.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceImplTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private BudgetServiceClient budgetServiceClient;

    @InjectMocks
    private ExpenseServiceImpl expenseService;

    private Expense expense;

    @BeforeEach
    void setUp() {
        expense = Expense.builder()
                .expenseId(1L)
                .userId(1L)
                .categoryId(100L)
                .title("Lunch")
                .amount(250.0)
                .date(LocalDate.now())
                .paymentMethod(Expense.PaymentMethod.CASH)
                .type(Expense.ExpenseType.EXPENSE)
                .currency("INR")
                .build();
    }

    @Test
    void addExpense_Success() {
        ExpenseDto.CreateExpenseRequest req = new ExpenseDto.CreateExpenseRequest();
        req.setCategoryId(100L);
        req.setTitle("Lunch");
        req.setAmount(250.0);
        req.setDate(LocalDate.now());

        when(expenseRepository.save(any(Expense.class))).thenReturn(expense);

        ExpenseDto.ExpenseResponse res = expenseService.addExpense(1L, req);

        assertNotNull(res);
        assertEquals("Lunch", res.getTitle());
        verify(budgetServiceClient, times(1)).getByCategory(eq(1L), eq(100L));
    }

    @Test
    void getExpenseById_Success() {
        when(expenseRepository.findByExpenseId(1L)).thenReturn(Optional.of(expense));
        ExpenseDto.ExpenseResponse res = expenseService.getExpenseById(1L, 1L);
        assertNotNull(res);
        assertEquals("Lunch", res.getTitle());
    }

    @Test
    void getExpensesByUser_Success() {
        when(expenseRepository.findByUserIdOrderByDateDesc(1L)).thenReturn(List.of(expense));
        List<ExpenseDto.ExpenseResponse> res = expenseService.getExpensesByUser(1L);
        assertEquals(1, res.size());
    }

    @Test
    void updateExpense_Success() {
        ExpenseDto.UpdateExpenseRequest req = new ExpenseDto.UpdateExpenseRequest();
        req.setAmount(300.0);
        req.setTitle("Big Lunch");

        when(expenseRepository.findByExpenseId(1L)).thenReturn(Optional.of(expense));
        when(expenseRepository.save(any(Expense.class))).thenReturn(expense);

        ExpenseDto.ExpenseResponse res = expenseService.updateExpense(1L, 1L, req);

        assertEquals("Big Lunch", expense.getTitle());
        assertEquals(300.0, expense.getAmount());
    }

    @Test
    void updateExpense_CategoryChanged() {
        ExpenseDto.UpdateExpenseRequest req = new ExpenseDto.UpdateExpenseRequest();
        req.setCategoryId(200L);

        when(expenseRepository.findByExpenseId(1L)).thenReturn(Optional.of(expense));
        when(expenseRepository.save(any(Expense.class))).thenReturn(expense);

        expenseService.updateExpense(1L, 1L, req);

        verify(budgetServiceClient, times(1)).getByCategory(eq(1L), eq(100L)); // old category rollback
        verify(budgetServiceClient, times(1)).getByCategory(eq(1L), eq(200L)); // new category update
    }

    @Test
    void deleteExpense_Success() {
        when(expenseRepository.findByExpenseId(1L)).thenReturn(Optional.of(expense));
        ExpenseDto.MessageResponse res = expenseService.deleteExpense(1L, 1L);
        assertTrue(res.isSuccess());
        verify(expenseRepository, times(1)).deleteByExpenseId(1L);
        verify(budgetServiceClient, times(1)).getByCategory(eq(1L), eq(100L)); // rollback budget
    }

    @Test
    void getTotalByUser_Success() {
        when(expenseRepository.sumAmountByUserId(1L)).thenReturn(1000.0);
        Double total = expenseService.getTotalByUser(1L);
        assertEquals(1000.0, total);
    }
    
    @Test
    void searchExpenses_Success() {
        when(expenseRepository.searchByKeyword(1L, "Lunch")).thenReturn(List.of(expense));
        List<ExpenseDto.ExpenseResponse> res = expenseService.searchExpenses(1L, "Lunch");
        assertEquals(1, res.size());
    }

    @Test
    void getExpensesByCategory_Success() {
        when(expenseRepository.findByUserIdAndCategoryId(1L, 100L)).thenReturn(List.of(expense));
        List<ExpenseDto.ExpenseResponse> res = expenseService.getExpensesByCategory(1L, 100L);
        assertEquals(1, res.size());
    }

    @Test
    void getExpensesByDateRange_Success() {
        when(expenseRepository.findByUserIdAndDateBetweenOrderByDateDesc(1L, LocalDate.now(), LocalDate.now())).thenReturn(List.of(expense));
        List<ExpenseDto.ExpenseResponse> res = expenseService.getExpensesByDateRange(1L, LocalDate.now(), LocalDate.now());
        assertEquals(1, res.size());
    }

    @Test
    void getExpensesByMonth_Success() {
        when(expenseRepository.findByUserIdAndMonth(1L, 4, 2026)).thenReturn(List.of(expense));
        List<ExpenseDto.ExpenseResponse> res = expenseService.getExpensesByMonth(1L, 4, 2026);
        assertEquals(1, res.size());
    }

    @Test
    void getExpensesByType_Success() {
        when(expenseRepository.findByUserIdAndType(1L, Expense.ExpenseType.EXPENSE)).thenReturn(List.of(expense));
        List<ExpenseDto.ExpenseResponse> res = expenseService.getExpensesByType(1L, "EXPENSE");
        assertEquals(1, res.size());
    }

    @Test
    void getExpensesByAmountRange_Success() {
        when(expenseRepository.findByUserIdAndAmountBetween(1L, 100.0, 5000.0)).thenReturn(List.of(expense));
        List<ExpenseDto.ExpenseResponse> res = expenseService.getExpensesByAmountRange(1L, 100.0, 5000.0);
        assertEquals(1, res.size());
    }

    @Test
    void getTotalByCategory_Success() {
        when(expenseRepository.sumAmountByUserIdAndCategoryId(1L, 100L)).thenReturn(200.0);
        Double total = expenseService.getTotalByCategory(1L, 100L);
        assertEquals(200.0, total);
    }

    @Test
    void getTotalByMonth_Success() {
        when(expenseRepository.sumAmountByUserIdAndMonth(1L, 4, 2026)).thenReturn(300.0);
        Double total = expenseService.getTotalByMonth(1L, 4, 2026);
        assertEquals(300.0, total);
    }

    @Test
    void getAllExpenses_Success() {
        when(expenseRepository.findAll()).thenReturn(List.of(expense));
        List<ExpenseDto.ExpenseResponse> res = expenseService.getAllExpenses();
        assertEquals(1, res.size());
    }
}
