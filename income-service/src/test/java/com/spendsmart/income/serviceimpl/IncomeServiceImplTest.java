package com.spendsmart.income.serviceimpl;

import com.spendsmart.income.dto.IncomeDto;
import com.spendsmart.income.entity.Income;
import com.spendsmart.income.repository.IncomeRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncomeServiceImplTest {

    @Mock
    private IncomeRepository incomeRepository;

    @InjectMocks
    private IncomeServiceImpl incomeService;

    private Income income;

    @BeforeEach
    void setUp() {
        income = Income.builder()
                .incomeId(1L)
                .userId(1L)
                .title("Salary")
                .amount(5000.0)
                .source(Income.IncomeSource.SALARY)
                .date(LocalDate.now())
                .currency("USD")
                .build();
    }

    @Test
    void addIncome_Success() {
        IncomeDto.CreateIncomeRequest req = new IncomeDto.CreateIncomeRequest();
        req.setTitle("Salary");
        req.setAmount(5000.0);
        req.setSource("SALARY");

        when(incomeRepository.save(any(Income.class))).thenReturn(income);

        IncomeDto.IncomeResponse res = incomeService.addIncome(1L, req);

        assertNotNull(res);
        assertEquals("Salary", res.getTitle());
        assertEquals(5000.0, res.getAmount());
    }

    @Test
    void getIncomeById_Success() {
        when(incomeRepository.findByIncomeId(1L)).thenReturn(Optional.of(income));
        IncomeDto.IncomeResponse res = incomeService.getIncomeById(1L, 1L);
        assertNotNull(res);
        assertEquals("Salary", res.getTitle());
    }

    @Test
    void updateIncome_Success() {
        IncomeDto.UpdateIncomeRequest req = new IncomeDto.UpdateIncomeRequest();
        req.setAmount(6000.0);

        when(incomeRepository.findByIncomeId(1L)).thenReturn(Optional.of(income));
        when(incomeRepository.save(any(Income.class))).thenReturn(income);

        IncomeDto.IncomeResponse res = incomeService.updateIncome(1L, 1L, req);

        assertEquals(6000.0, income.getAmount());
    }

    @Test
    void deleteIncome_Success() {
        when(incomeRepository.findByIncomeId(1L)).thenReturn(Optional.of(income));
        IncomeDto.MessageResponse res = incomeService.deleteIncome(1L, 1L);
        assertTrue(res.isSuccess());
        verify(incomeRepository, times(1)).deleteByIncomeId(1L);
    }

    @Test
    void getIncomesByUser_Success() {
        when(incomeRepository.findByUserIdOrderByDateDesc(1L)).thenReturn(List.of(income));
        List<IncomeDto.IncomeResponse> res = incomeService.getIncomesByUser(1L);
        assertEquals(1, res.size());
    }

    @Test
    void getBreakdownBySource_Success() {
        when(incomeRepository.findByUserIdOrderByDateDesc(1L)).thenReturn(List.of(income, income));
        List<IncomeDto.IncomeBreakdownBySource> res = incomeService.getBreakdownBySource(1L);
        
        assertEquals(1, res.size());
        assertEquals("SALARY", res.get(0).getSource());
        assertEquals(10000.0, res.get(0).getTotalAmount());
        assertEquals(100.0, res.get(0).getPercentage());
    }

    @Test
    void getTotalIncomeByUser_Success() {
        when(incomeRepository.sumAmountByUserId(1L)).thenReturn(5000.0);
        Double total = incomeService.getTotalIncomeByUser(1L);
        assertEquals(5000.0, total);
    }

    @Test
    void getIncomesBySource_Success() {
        when(incomeRepository.findByUserIdAndSource(1L, Income.IncomeSource.SALARY)).thenReturn(List.of(income));
        List<IncomeDto.IncomeResponse> res = incomeService.getIncomesBySource(1L, "SALARY");
        assertEquals(1, res.size());
    }

    @Test
    void getIncomesByDateRange_Success() {
        when(incomeRepository.findByUserIdAndDateBetweenOrderByDateDesc(1L, LocalDate.now(), LocalDate.now())).thenReturn(List.of(income));
        List<IncomeDto.IncomeResponse> res = incomeService.getIncomesByDateRange(1L, LocalDate.now(), LocalDate.now());
        assertEquals(1, res.size());
    }

    @Test
    void getIncomesByMonth_Success() {
        when(incomeRepository.findByUserIdAndMonth(1L, 4, 2026)).thenReturn(List.of(income));
        List<IncomeDto.IncomeResponse> res = incomeService.getIncomesByMonth(1L, 4, 2026);
        assertEquals(1, res.size());
    }

    @Test
    void getIncomesByCategory_Success() {
        when(incomeRepository.findByUserIdAndCategoryId(1L, 100L)).thenReturn(List.of(income));
        List<IncomeDto.IncomeResponse> res = incomeService.getIncomesByCategory(1L, 100L);
        assertEquals(1, res.size());
    }

    @Test
    void searchIncomes_Success() {
        when(incomeRepository.searchByKeyword(1L, "salary")).thenReturn(List.of(income));
        List<IncomeDto.IncomeResponse> res = incomeService.searchIncomes(1L, "salary");
        assertEquals(1, res.size());
    }

    @Test
    void getRecurringIncomes_Success() {
        when(incomeRepository.findByUserIdAndIsRecurring(1L, true)).thenReturn(List.of(income));
        List<IncomeDto.IncomeResponse> res = incomeService.getRecurringIncomes(1L);
        assertEquals(1, res.size());
    }

    @Test
    void getTotalIncomeByMonth_Success() {
        when(incomeRepository.sumAmountByUserIdAndMonth(1L, 4, 2026)).thenReturn(2000.0);
        Double total = incomeService.getTotalIncomeByMonth(1L, 4, 2026);
        assertEquals(2000.0, total);
    }

    @Test
    void getTotalIncomeBySource_Success() {
        when(incomeRepository.sumAmountByUserIdAndSource(1L, Income.IncomeSource.SALARY)).thenReturn(3000.0);
        Double total = incomeService.getTotalIncomeBySource(1L, "SALARY");
        assertEquals(3000.0, total);
    }
}
