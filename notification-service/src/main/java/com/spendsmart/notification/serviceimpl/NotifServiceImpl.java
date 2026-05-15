package com.spendsmart.notification.serviceimpl;

import com.spendsmart.notification.dto.NotificationDto;
import com.spendsmart.notification.entity.Notification;
import com.spendsmart.notification.entity.Notification.*;
import com.spendsmart.notification.repository.NotificationRepository;
import com.spendsmart.notification.service.NotifService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotifServiceImpl implements NotifService {

    private final NotificationRepository notificationRepository;

    // ─── Send ─────────────────────────────────────────────

    @Override
    @Transactional
    public NotificationDto.NotificationResponse send(NotificationDto.SendRequest req) {
        Notification n = Notification.builder()
                .recipientId(req.getRecipientId())
                .type(parseType(req.getType()))
                .severity(parseSeverity(req.getSeverity()))
                .title(req.getTitle())
                .message(req.getMessage())
                .relatedId(req.getRelatedId())
                .relatedType(req.getRelatedType())
                .isRead(false)
                .isAcknowledged(false)
                .build();
        return toResponse(notificationRepository.save(n));
    }

    @Override
    @Transactional
    public NotificationDto.MessageResponse sendBudgetAlert(NotificationDto.BudgetAlertRequest req) {
        boolean exceeded = Boolean.TRUE.equals(req.getExceeded());
        Severity sev = exceeded ? Severity.CRITICAL : Severity.WARNING;
        String title = exceeded
                ? "🚨 Budget Exceeded: " + req.getBudgetName()
                : "⚠️ Budget Alert: " + req.getBudgetName();
        String msg = String.format(
                "You have %s %.0f%% of your %s budget (₹%.2f / ₹%.2f).",
                exceeded ? "exceeded" : "used",
                req.getPercentageUsed(),
                req.getBudgetName(),
                req.getSpentAmount(),
                req.getLimitAmount()
        );

        Notification n = Notification.builder()
                .recipientId(req.getRecipientId())
                .type(exceeded ? NotificationType.BUDGET_EXCEEDED : NotificationType.BUDGET_ALERT)
                .severity(sev)
                .title(title)
                .message(msg)
                .relatedId(req.getBudgetId())
                .relatedType("BUDGET")
                .isRead(false)
                .isAcknowledged(false)
                .build();

        notificationRepository.save(n);
        log.info("Budget alert sent to user {} for budget {}", req.getRecipientId(), req.getBudgetName());

        // In production: also dispatch email via JavaMailSender for CRITICAL alerts
        if (exceeded) {
            log.info("CRITICAL budget alert — email dispatch would trigger here for user {}", req.getRecipientId());
        }

        return new NotificationDto.MessageResponse("Budget alert sent", true);
    }

    @Override
    @Transactional
    public List<NotificationDto.NotificationResponse> sendBulk(NotificationDto.BulkRequest req) {
        return req.getRecipientIds().stream().map(recipientId -> {
            NotificationDto.SendRequest sr = new NotificationDto.SendRequest();
            sr.setRecipientId(recipientId);
            sr.setType(req.getType());
            sr.setSeverity(req.getSeverity());
            sr.setTitle(req.getTitle());
            sr.setMessage(req.getMessage());
            return send(sr);
        }).collect(Collectors.toList());
    }

    // ─── Read / Acknowledge ───────────────────────────────

    @Override
    @Transactional
    public NotificationDto.NotificationResponse markAsRead(Long notificationId) {
        Notification n = findById(notificationId);
        n.setIsRead(true);
        return toResponse(notificationRepository.save(n));
    }

    @Override
    @Transactional
    public NotificationDto.MessageResponse markAllRead(Long recipientId) {
        notificationRepository.markAllReadByRecipient(recipientId);
        return new NotificationDto.MessageResponse("All notifications marked as read", true);
    }

    @Override
    @Transactional
    public NotificationDto.NotificationResponse acknowledge(Long notificationId) {
        Notification n = findById(notificationId);
        n.setIsRead(true);
        n.setIsAcknowledged(true);
        return toResponse(notificationRepository.save(n));
    }

    // ─── Queries ──────────────────────────────────────────

    @Override
    public List<NotificationDto.NotificationResponse> getByRecipient(Long recipientId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<NotificationDto.NotificationResponse> getUnread(Long recipientId) {
        return notificationRepository.findByRecipientIdAndIsRead(recipientId, false)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public NotificationDto.UnreadCountResponse getUnreadCount(Long recipientId) {
        return new NotificationDto.UnreadCountResponse(
                notificationRepository.countByRecipientIdAndIsRead(recipientId, false));
    }

    @Override
    @Transactional
    public NotificationDto.MessageResponse deleteNotification(Long notificationId) {
        findById(notificationId);
        notificationRepository.deleteByNotificationId(notificationId);
        return new NotificationDto.MessageResponse("Notification deleted", true);
    }

    @Override
    public List<NotificationDto.NotificationResponse> getAll() {
        return notificationRepository.findAll()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ─── Helpers ──────────────────────────────────────────

    private Notification findById(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + id));
    }

    private NotificationType parseType(String t) {
        try { return t != null ? NotificationType.valueOf(t.toUpperCase()) : NotificationType.SYSTEM; }
        catch (Exception e) { return NotificationType.SYSTEM; }
    }

    private Severity parseSeverity(String s) {
        try { return s != null ? Severity.valueOf(s.toUpperCase()) : Severity.INFO; }
        catch (Exception e) { return Severity.INFO; }
    }

    private NotificationDto.NotificationResponse toResponse(Notification n) {
        NotificationDto.NotificationResponse r = new NotificationDto.NotificationResponse();
        r.setNotificationId(n.getNotificationId());
        r.setRecipientId(n.getRecipientId());
        r.setType(n.getType().name());
        r.setSeverity(n.getSeverity().name());
        r.setTitle(n.getTitle());
        r.setMessage(n.getMessage());
        r.setRelatedId(n.getRelatedId());
        r.setRelatedType(n.getRelatedType());
        r.setIsRead(n.getIsRead());
        r.setIsAcknowledged(n.getIsAcknowledged());
        r.setCreatedAt(n.getCreatedAt() != null ? n.getCreatedAt().toString() : null);
        return r;
    }
}
