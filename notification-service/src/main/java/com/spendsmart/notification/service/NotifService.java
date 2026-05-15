package com.spendsmart.notification.service;

import com.spendsmart.notification.dto.NotificationDto;
import java.util.List;

public interface NotifService {
    NotificationDto.NotificationResponse send(NotificationDto.SendRequest req);
    NotificationDto.MessageResponse sendBudgetAlert(NotificationDto.BudgetAlertRequest req);
    List<NotificationDto.NotificationResponse> sendBulk(NotificationDto.BulkRequest req);
    NotificationDto.NotificationResponse markAsRead(Long notificationId);
    NotificationDto.MessageResponse markAllRead(Long recipientId);
    NotificationDto.NotificationResponse acknowledge(Long notificationId);
    List<NotificationDto.NotificationResponse> getByRecipient(Long recipientId);
    List<NotificationDto.NotificationResponse> getUnread(Long recipientId);
    NotificationDto.UnreadCountResponse getUnreadCount(Long recipientId);
    NotificationDto.MessageResponse deleteNotification(Long notificationId);
    List<NotificationDto.NotificationResponse> getAll();
}
