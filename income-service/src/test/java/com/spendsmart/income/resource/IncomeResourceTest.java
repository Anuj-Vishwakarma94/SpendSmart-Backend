package com.spendsmart.income.resource;

import com.spendsmart.income.dto.IncomeDto;
import com.spendsmart.income.service.IncomeService;
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
class IncomeResourceTest {

    @Mock
    private IncomeService incomeService;

    @InjectMocks
    private IncomeResource incomeResource;

    private Authentication auth;
    private Long userId = 1L;

    @BeforeEach
    void setUp() {
        auth = mock(Authentication.class);
        org.mockito.Mockito.lenient().when(auth.getDetails()).thenReturn(userId);
    }

    @Test
    void add_Success() {
        IncomeDto.CreateIncomeRequest req = new IncomeDto.CreateIncomeRequest();
        IncomeDto.IncomeResponse res = new IncomeDto.IncomeResponse();
        res.setIncomeId(1L);

        when(incomeService.addIncome(eq(userId), any())).thenReturn(res);

        ResponseEntity<IncomeDto.IncomeResponse> response = incomeResource.add(auth, req);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getIncomeId());
    }

    @Test
    void getById_Success() {
        when(incomeService.getIncomeById(1L, userId)).thenReturn(new IncomeDto.IncomeResponse());
        ResponseEntity<IncomeDto.IncomeResponse> response = incomeResource.getById(auth, 1L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void update_Success() {
        IncomeDto.UpdateIncomeRequest req = new IncomeDto.UpdateIncomeRequest();
        when(incomeService.updateIncome(1L, userId, req)).thenReturn(new IncomeDto.IncomeResponse());
        ResponseEntity<IncomeDto.IncomeResponse> response = incomeResource.update(auth, 1L, req);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void delete_Success() {
        IncomeDto.MessageResponse msg = new IncomeDto.MessageResponse("Deleted", true);
        when(incomeService.deleteIncome(1L, userId)).thenReturn(msg);
        ResponseEntity<IncomeDto.MessageResponse> response = incomeResource.delete(auth, 1L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getAll_Success() {
        when(incomeService.getIncomesByUser(userId)).thenReturn(List.of(new IncomeDto.IncomeResponse()));
        ResponseEntity<List<IncomeDto.IncomeResponse>> response = incomeResource.getAll(auth);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getBySource_Success() {
        when(incomeService.getIncomesBySource(userId, "SALARY")).thenReturn(List.of(new IncomeDto.IncomeResponse()));
        ResponseEntity<List<IncomeDto.IncomeResponse>> response = incomeResource.getBySource(auth, "SALARY");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getByDateRange_Success() {
        when(incomeService.getIncomesByDateRange(userId, LocalDate.now(), LocalDate.now())).thenReturn(List.of(new IncomeDto.IncomeResponse()));
        ResponseEntity<List<IncomeDto.IncomeResponse>> response = incomeResource.getByDateRange(auth, LocalDate.now(), LocalDate.now());
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getByMonth_Success() {
        when(incomeService.getIncomesByMonth(userId, 4, 2026)).thenReturn(List.of(new IncomeDto.IncomeResponse()));
        ResponseEntity<List<IncomeDto.IncomeResponse>> response = incomeResource.getByMonth(auth, 4, 2026);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getByCategory_Success() {
        when(incomeService.getIncomesByCategory(userId, 100L)).thenReturn(List.of(new IncomeDto.IncomeResponse()));
        ResponseEntity<List<IncomeDto.IncomeResponse>> response = incomeResource.getByCategory(auth, 100L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void search_Success() {
        when(incomeService.searchIncomes(userId, "salary")).thenReturn(List.of(new IncomeDto.IncomeResponse()));
        ResponseEntity<List<IncomeDto.IncomeResponse>> response = incomeResource.search(auth, "salary");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getRecurring_Success() {
        when(incomeService.getRecurringIncomes(userId)).thenReturn(List.of(new IncomeDto.IncomeResponse()));
        ResponseEntity<List<IncomeDto.IncomeResponse>> response = incomeResource.getRecurring(auth);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getTotal_Success() {
        when(incomeService.getTotalIncomeByUser(userId)).thenReturn(5000.0);
        ResponseEntity<Map<String, Double>> response = incomeResource.getTotal(auth);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(5000.0, response.getBody().get("total"));
    }

    @Test
    void getTotalByMonth_Success() {
        when(incomeService.getTotalIncomeByMonth(userId, 4, 2026)).thenReturn(3000.0);
        ResponseEntity<Map<String, Double>> response = incomeResource.getTotalByMonth(auth, 4, 2026);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getTotalBySource_Success() {
        when(incomeService.getTotalIncomeBySource(userId, "SALARY")).thenReturn(2000.0);
        ResponseEntity<Map<String, Double>> response = incomeResource.getTotalBySource(auth, "SALARY");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getSourceBreakdown_Success() {
        when(incomeService.getBreakdownBySource(userId)).thenReturn(List.of(new IncomeDto.IncomeBreakdownBySource()));
        ResponseEntity<List<IncomeDto.IncomeBreakdownBySource>> response = incomeResource.getSourceBreakdown(auth);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
