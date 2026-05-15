package com.spendsmart.notification.resource;

import com.spendsmart.notification.dto.NotificationDto;
import com.spendsmart.notification.service.NotifService;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotifResourceTest {

    @Mock
    private NotifService notifService;

    @InjectMocks
    private NotifResource notifResource;

    private Authentication auth;
    private Long userId = 1L;

    @BeforeEach
    void setUp() {
        auth = mock(Authentication.class);
        org.mockito.Mockito.lenient().when(auth.getDetails()).thenReturn(userId);
    }

    @Test
    void getAll_Success() {
        when(notifService.getByRecipient(userId)).thenReturn(List.of(new NotificationDto.NotificationResponse()));
        ResponseEntity<List<NotificationDto.NotificationResponse>> res = notifResource.getAll(auth);
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void getUnread_Success() {
        when(notifService.getUnread(userId)).thenReturn(List.of(new NotificationDto.NotificationResponse()));
        ResponseEntity<List<NotificationDto.NotificationResponse>> res = notifResource.getUnread(auth);
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void getUnreadCount_Success() {
        when(notifService.getUnreadCount(userId)).thenReturn(new NotificationDto.UnreadCountResponse(5L));
        ResponseEntity<NotificationDto.UnreadCountResponse> res = notifResource.getUnreadCount(auth);
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void send_Success() {
        when(notifService.send(any())).thenReturn(new NotificationDto.NotificationResponse());
        ResponseEntity<NotificationDto.NotificationResponse> res = notifResource.send(new NotificationDto.SendRequest());
        assertEquals(HttpStatus.CREATED, res.getStatusCode());
    }

    @Test
    void budgetAlert_Success() {
        when(notifService.sendBudgetAlert(any())).thenReturn(new NotificationDto.MessageResponse("msg", true));
        ResponseEntity<NotificationDto.MessageResponse> res = notifResource.budgetAlert(new NotificationDto.BudgetAlertRequest());
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void bulk_Success() {
        when(notifService.sendBulk(any())).thenReturn(List.of(new NotificationDto.NotificationResponse()));
        ResponseEntity<List<NotificationDto.NotificationResponse>> res = notifResource.bulk(new NotificationDto.BulkRequest());
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void markRead_Success() {
        when(notifService.markAsRead(1L)).thenReturn(new NotificationDto.NotificationResponse());
        ResponseEntity<NotificationDto.NotificationResponse> res = notifResource.markRead(1L);
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void markAllRead_Success() {
        when(notifService.markAllRead(userId)).thenReturn(new NotificationDto.MessageResponse("msg", true));
        ResponseEntity<NotificationDto.MessageResponse> res = notifResource.markAllRead(auth);
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void acknowledge_Success() {
        when(notifService.acknowledge(1L)).thenReturn(new NotificationDto.NotificationResponse());
        ResponseEntity<NotificationDto.NotificationResponse> res = notifResource.acknowledge(1L);
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void delete_Success() {
        when(notifService.deleteNotification(1L)).thenReturn(new NotificationDto.MessageResponse("msg", true));
        ResponseEntity<NotificationDto.MessageResponse> res = notifResource.delete(1L);
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void getAllAdmin_Success() {
        when(notifService.getAll()).thenReturn(List.of(new NotificationDto.NotificationResponse()));
        ResponseEntity<List<NotificationDto.NotificationResponse>> res = notifResource.getAllAdmin();
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }
}
