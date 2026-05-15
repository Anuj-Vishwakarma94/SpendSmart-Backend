package com.spendsmart.expense.resource;

import com.spendsmart.expense.dto.ExpenseDto;
import com.spendsmart.expense.service.ExpenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseResourceTest {

    @Mock
    private ExpenseService expenseService;

    @InjectMocks
    private ExpenseResource expenseResource;

    private Authentication auth;
    private Long userId = 1L;

    @BeforeEach
    void setUp() {
        auth = mock(Authentication.class);
        org.mockito.Mockito.lenient().when(auth.getDetails()).thenReturn(userId);
    }

    @Test
    void addExpense_Success() {
        ExpenseDto.CreateExpenseRequest req = new ExpenseDto.CreateExpenseRequest();
        ExpenseDto.ExpenseResponse res = new ExpenseDto.ExpenseResponse();
        res.setExpenseId(1L);

        when(expenseService.addExpense(eq(userId), any())).thenReturn(res);

        ResponseEntity<ExpenseDto.ExpenseResponse> response = expenseResource.addExpense(auth, req);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getExpenseId());
    }

    @Test
    void getAllAdmin_Success() {
        when(expenseService.getAllExpenses()).thenReturn(List.of(new ExpenseDto.ExpenseResponse()));
        ResponseEntity<List<ExpenseDto.ExpenseResponse>> response = expenseResource.getAllAdmin();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getById_Success() {
        when(expenseService.getExpenseById(1L, userId)).thenReturn(new ExpenseDto.ExpenseResponse());
        ResponseEntity<ExpenseDto.ExpenseResponse> response = expenseResource.getById(auth, 1L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void getAll_Success() {
        when(expenseService.getExpensesByUser(userId)).thenReturn(List.of(new ExpenseDto.ExpenseResponse()));
        ResponseEntity<List<ExpenseDto.ExpenseResponse>> response = expenseResource.getAll(auth);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getByCategory_Success() {
        when(expenseService.getExpensesByCategory(userId, 100L)).thenReturn(List.of(new ExpenseDto.ExpenseResponse()));
        ResponseEntity<List<ExpenseDto.ExpenseResponse>> response = expenseResource.getByCategory(auth, 100L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getByDateRange_Success() {
        when(expenseService.getExpensesByDateRange(userId, LocalDate.now(), LocalDate.now())).thenReturn(List.of(new ExpenseDto.ExpenseResponse()));
        ResponseEntity<List<ExpenseDto.ExpenseResponse>> response = expenseResource.getByDateRange(auth, LocalDate.now(), LocalDate.now());
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getByMonth_Success() {
        when(expenseService.getExpensesByMonth(userId, 4, 2026)).thenReturn(List.of(new ExpenseDto.ExpenseResponse()));
        ResponseEntity<List<ExpenseDto.ExpenseResponse>> response = expenseResource.getByMonth(auth, 4, 2026);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getByType_Success() {
        when(expenseService.getExpensesByType(userId, "EXPENSE")).thenReturn(List.of(new ExpenseDto.ExpenseResponse()));
        ResponseEntity<List<ExpenseDto.ExpenseResponse>> response = expenseResource.getByType(auth, "EXPENSE");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void search_Success() {
        when(expenseService.searchExpenses(userId, "food")).thenReturn(List.of(new ExpenseDto.ExpenseResponse()));
        ResponseEntity<List<ExpenseDto.ExpenseResponse>> response = expenseResource.search(auth, "food");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getByAmountRange_Success() {
        when(expenseService.getExpensesByAmountRange(userId, 100.0, 5000.0)).thenReturn(List.of(new ExpenseDto.ExpenseResponse()));
        ResponseEntity<List<ExpenseDto.ExpenseResponse>> response = expenseResource.getByAmountRange(auth, 100.0, 5000.0);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void update_Success() {
        ExpenseDto.UpdateExpenseRequest req = new ExpenseDto.UpdateExpenseRequest();
        when(expenseService.updateExpense(1L, userId, req)).thenReturn(new ExpenseDto.ExpenseResponse());
        ResponseEntity<ExpenseDto.ExpenseResponse> response = expenseResource.update(auth, 1L, req);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void delete_Success() {
        ExpenseDto.MessageResponse msg = new ExpenseDto.MessageResponse("Deleted", true);
        when(expenseService.deleteExpense(1L, userId)).thenReturn(msg);
        ResponseEntity<ExpenseDto.MessageResponse> response = expenseResource.delete(auth, 1L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getTotal_Success() {
        when(expenseService.getTotalByUser(userId)).thenReturn(500.0);
        ResponseEntity<Map<String, Double>> response = expenseResource.getTotal(auth);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(500.0, response.getBody().get("total"));
    }

    @Test
    void getTotalByCategory_Success() {
        when(expenseService.getTotalByCategory(userId, 100L)).thenReturn(200.0);
        ResponseEntity<Map<String, Double>> response = expenseResource.getTotalByCategory(auth, 100L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getTotalByMonth_Success() {
        when(expenseService.getTotalByMonth(userId, 4, 2026)).thenReturn(300.0);
        ResponseEntity<Map<String, Double>> response = expenseResource.getTotalByMonth(auth, 4, 2026);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
