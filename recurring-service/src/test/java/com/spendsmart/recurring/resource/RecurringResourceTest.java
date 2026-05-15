package com.spendsmart.recurring.resource;

import com.spendsmart.recurring.dto.RecurringDto;
import com.spendsmart.recurring.service.RecurringService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringResourceTest {

    @Mock
    private RecurringService recurringService;

    @InjectMocks
    private RecurringResource recurringResource;

    private Long userId = 1L;

    private Authentication mockAuth() {
        Authentication auth = mock(Authentication.class);
        when(auth.getDetails()).thenReturn(userId);
        return auth;
    }

    @Test
    void add() {
        when(recurringService.addRecurring(eq(userId), any())).thenReturn(new RecurringDto.RecurringResponse());
        ResponseEntity<RecurringDto.RecurringResponse> res = recurringResource.add(mockAuth(), new RecurringDto.CreateRequest());
        assertEquals(HttpStatus.CREATED, res.getStatusCode());
    }

    @Test
    void getById() {
        when(recurringService.getById(1L, userId)).thenReturn(new RecurringDto.RecurringResponse());
        ResponseEntity<RecurringDto.RecurringResponse> res = recurringResource.getById(mockAuth(), 1L);
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void getAll() {
        when(recurringService.getByUser(userId)).thenReturn(List.of(new RecurringDto.RecurringResponse()));
        ResponseEntity<List<RecurringDto.RecurringResponse>> res = recurringResource.getAll(mockAuth());
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void getActive() {
        when(recurringService.getActiveRecurring(userId)).thenReturn(List.of(new RecurringDto.RecurringResponse()));
        ResponseEntity<List<RecurringDto.RecurringResponse>> res = recurringResource.getActive(mockAuth());
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void getByType() {
        when(recurringService.getByType(userId, "EXPENSE")).thenReturn(List.of(new RecurringDto.RecurringResponse()));
        ResponseEntity<List<RecurringDto.RecurringResponse>> res = recurringResource.getByType(mockAuth(), "EXPENSE");
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void getUpcomingThisMonth() {
        when(recurringService.getUpcomingThisMonth(userId)).thenReturn(List.of(new RecurringDto.RecurringResponse()));
        ResponseEntity<List<RecurringDto.RecurringResponse>> res = recurringResource.getUpcomingThisMonth(mockAuth());
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void getDueSoon() {
        when(recurringService.getDueWithinDays(3)).thenReturn(List.of(new RecurringDto.RecurringResponse()));
        ResponseEntity<List<RecurringDto.RecurringResponse>> res = recurringResource.getDueSoon(3);
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void update() {
        when(recurringService.updateRecurring(eq(1L), eq(userId), any())).thenReturn(new RecurringDto.RecurringResponse());
        ResponseEntity<RecurringDto.RecurringResponse> res = recurringResource.update(mockAuth(), 1L, new RecurringDto.UpdateRequest());
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void deactivate() {
        when(recurringService.deactivateRecurring(1L, userId)).thenReturn(new RecurringDto.MessageResponse("msg", true));
        ResponseEntity<RecurringDto.MessageResponse> res = recurringResource.deactivate(mockAuth(), 1L);
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void processManual() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer token");
        when(recurringService.processManualPayment(1L, userId, "Bearer token")).thenReturn(new RecurringDto.MessageResponse("msg", true));
        ResponseEntity<RecurringDto.MessageResponse> res = recurringResource.processManual(mockAuth(), 1L, req);
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void delete() {
        when(recurringService.deleteRecurring(1L, userId)).thenReturn(new RecurringDto.MessageResponse("msg", true));
        ResponseEntity<RecurringDto.MessageResponse> res = recurringResource.delete(mockAuth(), 1L);
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }
}
