package com.spendsmart.recurring.serviceimpl;

import com.spendsmart.recurring.dto.RecurringDto;
import com.spendsmart.recurring.entity.RecurringTransaction;
import com.spendsmart.recurring.repository.RecurringRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringServiceImplTest {

    @Mock
    private RecurringRepository recurringRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private RecurringServiceImpl recurringService;

    private RecurringTransaction rt;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(recurringService, "expenseUrl", "http://expense");
        ReflectionTestUtils.setField(recurringService, "incomeUrl", "http://income");

        rt = RecurringTransaction.builder()
                .recurringId(1L)
                .userId(1L)
                .categoryId(1L)
                .title("Rent")
                .amount(500.0)
                .type(RecurringTransaction.TransactionType.EXPENSE)
                .frequency(RecurringTransaction.Frequency.MONTHLY)
                .startDate(LocalDate.now().minusDays(1))
                .nextDueDate(LocalDate.now())
                .isActive(true)
                .paymentMethod(RecurringTransaction.PaymentMethod.UPI)
                .currency("INR")
                .build();
    }

    @Test
    void addRecurring_Success() {
        RecurringDto.CreateRequest req = new RecurringDto.CreateRequest();
        req.setStartDate(LocalDate.now().toString());
        req.setType("EXPENSE");
        req.setFrequency("MONTHLY");
        req.setAmount(500.0);
        req.setTitle("Rent");

        when(recurringRepository.save(any(RecurringTransaction.class))).thenReturn(rt);

        RecurringDto.RecurringResponse res = recurringService.addRecurring(1L, req);
        assertNotNull(res);
        assertEquals("Rent", res.getTitle());
    }

    @Test
    void getById_Success() {
        when(recurringRepository.findByRecurringId(1L)).thenReturn(Optional.of(rt));
        RecurringDto.RecurringResponse res = recurringService.getById(1L, 1L);
        assertNotNull(res);
    }

    @Test
    void getByUser_Success() {
        when(recurringRepository.findByUserId(1L)).thenReturn(List.of(rt));
        assertEquals(1, recurringService.getByUser(1L).size());
    }

    @Test
    void getActiveRecurring_Success() {
        when(recurringRepository.findByUserIdAndIsActive(1L, true)).thenReturn(List.of(rt));
        assertEquals(1, recurringService.getActiveRecurring(1L).size());
    }

    @Test
    void getByType_Success() {
        when(recurringRepository.findByUserIdAndType(1L, RecurringTransaction.TransactionType.EXPENSE)).thenReturn(List.of(rt));
        assertEquals(1, recurringService.getByType(1L, "EXPENSE").size());
    }

    @Test
    void updateRecurring_Success() {
        when(recurringRepository.findByRecurringId(1L)).thenReturn(Optional.of(rt));
        when(recurringRepository.save(any(RecurringTransaction.class))).thenReturn(rt);

        RecurringDto.UpdateRequest req = new RecurringDto.UpdateRequest();
        req.setTitle("Updated Rent");

        RecurringDto.RecurringResponse res = recurringService.updateRecurring(1L, 1L, req);
        assertNotNull(res);
    }

    @Test
    void deactivateRecurring_Success() {
        when(recurringRepository.findByRecurringId(1L)).thenReturn(Optional.of(rt));
        when(recurringRepository.save(any(RecurringTransaction.class))).thenReturn(rt);

        RecurringDto.MessageResponse res = recurringService.deactivateRecurring(1L, 1L);
        assertTrue(res.isSuccess());
        assertFalse(rt.getIsActive());
    }

    @Test
    void deleteRecurring_Success() {
        when(recurringRepository.findByRecurringId(1L)).thenReturn(Optional.of(rt));
        RecurringDto.MessageResponse res = recurringService.deleteRecurring(1L, 1L);
        assertTrue(res.isSuccess());
        verify(recurringRepository, times(1)).deleteByRecurringId(1L);
    }

    @Test
    void processManualPayment_Success() {
        when(recurringRepository.findByRecurringId(1L)).thenReturn(Optional.of(rt));
        when(restTemplate.postForObject(anyString(), any(), eq(java.util.Map.class))).thenReturn(new java.util.HashMap());

        RecurringDto.MessageResponse res = recurringService.processManualPayment(1L, 1L, "token");
        assertTrue(res.isSuccess());
    }

    @Test
    void processManualPayment_Income_Success() {
        rt.setType(RecurringTransaction.TransactionType.INCOME);
        when(recurringRepository.findByRecurringId(1L)).thenReturn(Optional.of(rt));
        when(restTemplate.postForObject(anyString(), any(), eq(java.util.Map.class))).thenReturn(new java.util.HashMap());

        RecurringDto.MessageResponse res = recurringService.processManualPayment(1L, 1L, "token");
        assertTrue(res.isSuccess());
    }

    @Test
    void processManualPayment_NotDue() {
        rt.setNextDueDate(LocalDate.now().plusDays(1));
        when(recurringRepository.findByRecurringId(1L)).thenReturn(Optional.of(rt));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> recurringService.processManualPayment(1L, 1L, "token"));
        assertTrue(ex.getMessage().contains("not yet due"));
    }

    @Test
    void processManualPayment_Inactive() {
        rt.setIsActive(false);
        when(recurringRepository.findByRecurringId(1L)).thenReturn(Optional.of(rt));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> recurringService.processManualPayment(1L, 1L, "token"));
        assertTrue(ex.getMessage().contains("inactive"));
    }

    @Test
    void processUpcomingDue_Success() {
        when(recurringRepository.findByIsActiveAndNextDueDateLessThanEqual(true, LocalDate.now())).thenReturn(List.of(rt));
        when(restTemplate.postForObject(anyString(), any(), eq(java.util.Map.class))).thenReturn(new java.util.HashMap());

        recurringService.processUpcomingDue();
        verify(recurringRepository, times(1)).save(rt);
    }

    @Test
    void processUpcomingDue_EndDatePassed() {
        rt.setEndDate(LocalDate.now().minusDays(1)); // it is active but end date is past
        when(recurringRepository.findByIsActiveAndNextDueDateLessThanEqual(true, LocalDate.now())).thenReturn(List.of(rt));

        recurringService.processUpcomingDue();
        assertFalse(rt.getIsActive());
        verify(recurringRepository, times(1)).save(rt);
    }

    @Test
    void getUpcomingThisMonth_Success() {
        when(recurringRepository.findDueThisMonth(1L)).thenReturn(List.of(rt));
        assertEquals(1, recurringService.getUpcomingThisMonth(1L).size());
    }

    @Test
    void getDueWithinDays_Success() {
        when(recurringRepository.findDueWithinDays(any())).thenReturn(List.of(rt));
        assertEquals(1, recurringService.getDueWithinDays(3).size());
    }
}
