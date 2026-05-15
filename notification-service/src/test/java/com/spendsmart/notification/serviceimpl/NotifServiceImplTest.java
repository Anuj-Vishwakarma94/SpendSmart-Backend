package com.spendsmart.notification.serviceimpl;

import com.spendsmart.notification.dto.NotificationDto;
import com.spendsmart.notification.entity.Notification;
import com.spendsmart.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotifServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotifServiceImpl notifService;

    private Notification notification;

    @BeforeEach
    void setUp() {
        notification = Notification.builder()
                .notificationId(1L)
                .recipientId(1L)
                .type(Notification.NotificationType.SYSTEM)
                .severity(Notification.Severity.INFO)
                .title("Test")
                .message("Message")
                .isRead(false)
                .isAcknowledged(false)
                .build();
    }

    @Test
    void send_Success() {
        NotificationDto.SendRequest req = new NotificationDto.SendRequest();
        req.setRecipientId(1L);
        req.setTitle("Test");
        req.setType("SYSTEM");
        req.setSeverity("INFO");

        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        NotificationDto.NotificationResponse res = notifService.send(req);

        assertNotNull(res);
        assertEquals("Test", res.getTitle());
    }

    @Test
    void sendBudgetAlert_Success() {
        NotificationDto.BudgetAlertRequest req = new NotificationDto.BudgetAlertRequest();
        req.setRecipientId(1L);
        req.setBudgetName("Food");
        req.setPercentageUsed(110.0);
        req.setSpentAmount(1100.0);
        req.setLimitAmount(1000.0);
        req.setExceeded(true);

        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        NotificationDto.MessageResponse res = notifService.sendBudgetAlert(req);
        assertTrue(res.isSuccess());
    }

    @Test
    void sendBulk_Success() {
        NotificationDto.BulkRequest req = new NotificationDto.BulkRequest();
        req.setRecipientIds(List.of(1L, 2L));
        req.setTitle("Bulk");

        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        List<NotificationDto.NotificationResponse> res = notifService.sendBulk(req);
        assertEquals(2, res.size());
    }

    @Test
    void markAsRead_Success() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        NotificationDto.NotificationResponse res = notifService.markAsRead(1L);
        assertTrue(notification.getIsRead());
        assertNotNull(res);
    }

    @Test
    void markAllRead_Success() {
        doNothing().when(notificationRepository).markAllReadByRecipient(1L);
        NotificationDto.MessageResponse res = notifService.markAllRead(1L);
        assertTrue(res.isSuccess());
    }

    @Test
    void acknowledge_Success() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        NotificationDto.NotificationResponse res = notifService.acknowledge(1L);
        assertTrue(notification.getIsAcknowledged());
    }

    @Test
    void getByRecipient_Success() {
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(notification));
        List<NotificationDto.NotificationResponse> res = notifService.getByRecipient(1L);
        assertEquals(1, res.size());
    }

    @Test
    void getUnread_Success() {
        when(notificationRepository.findByRecipientIdAndIsRead(1L, false)).thenReturn(List.of(notification));
        List<NotificationDto.NotificationResponse> res = notifService.getUnread(1L);
        assertEquals(1, res.size());
    }

    @Test
    void getUnreadCount_Success() {
        when(notificationRepository.countByRecipientIdAndIsRead(1L, false)).thenReturn(5L);
        NotificationDto.UnreadCountResponse res = notifService.getUnreadCount(1L);
        assertEquals(5L, res.getCount());
    }

    @Test
    void deleteNotification_Success() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        doNothing().when(notificationRepository).deleteByNotificationId(1L);

        NotificationDto.MessageResponse res = notifService.deleteNotification(1L);
        assertTrue(res.isSuccess());
    }

    @Test
    void getAll_Success() {
        when(notificationRepository.findAll()).thenReturn(List.of(notification));
        List<NotificationDto.NotificationResponse> res = notifService.getAll();
        assertEquals(1, res.size());
    }
}
