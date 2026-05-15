package com.spendsmart.notification.repository;

import com.spendsmart.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);
    List<Notification> findByRecipientIdAndIsRead(Long recipientId, Boolean isRead);
    long countByRecipientIdAndIsRead(Long recipientId, Boolean isRead);
    List<Notification> findByType(Notification.NotificationType type);
    List<Notification> findBySeverity(Notification.Severity severity);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipientId = :id")
    void markAllReadByRecipient(@Param("id") Long recipientId);

    void deleteByNotificationId(Long notificationId);
}
