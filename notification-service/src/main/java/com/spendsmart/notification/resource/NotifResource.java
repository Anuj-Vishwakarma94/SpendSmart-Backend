package com.spendsmart.notification.resource;

import com.spendsmart.notification.dto.NotificationDto;
import com.spendsmart.notification.service.NotifService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotifResource {

    private final NotifService notifService;
    private Long uid(Authentication a) { return (Long) a.getDetails(); }

    // GET /api/notifications  — all notifications for current user
    @GetMapping
    public ResponseEntity<List<NotificationDto.NotificationResponse>> getAll(Authentication a) {
        return ResponseEntity.ok(notifService.getByRecipient(uid(a)));
    }

    // GET /api/notifications/unread
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationDto.NotificationResponse>> getUnread(Authentication a) {
        return ResponseEntity.ok(notifService.getUnread(uid(a)));
    }

    // GET /api/notifications/unread-count
    @GetMapping("/unread-count")
    public ResponseEntity<NotificationDto.UnreadCountResponse> getUnreadCount(Authentication a) {
        return ResponseEntity.ok(notifService.getUnreadCount(uid(a)));
    }

    // POST /api/notifications  — send a single notification (admin / internal)
    @PostMapping
    public ResponseEntity<NotificationDto.NotificationResponse> send(
            @RequestBody NotificationDto.SendRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notifService.send(req));
    }

    // POST /api/notifications/budget-alert  — triggered by budget-service
    @PostMapping("/budget-alert")
    public ResponseEntity<NotificationDto.MessageResponse> budgetAlert(
            @RequestBody NotificationDto.BudgetAlertRequest req) {
        return ResponseEntity.ok(notifService.sendBudgetAlert(req));
    }

    // POST /api/notifications/bulk  — broadcast from admin panel
    @PostMapping("/bulk")
    public ResponseEntity<List<NotificationDto.NotificationResponse>> bulk(
            @RequestBody NotificationDto.BulkRequest req) {
        return ResponseEntity.ok(notifService.sendBulk(req));
    }

    // PUT /api/notifications/{id}/read
    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationDto.NotificationResponse> markRead(@PathVariable Long id) {
        return ResponseEntity.ok(notifService.markAsRead(id));
    }

    // PUT /api/notifications/read-all
    @PutMapping("/read-all")
    public ResponseEntity<NotificationDto.MessageResponse> markAllRead(Authentication a) {
        return ResponseEntity.ok(notifService.markAllRead(uid(a)));
    }

    // PUT /api/notifications/{id}/acknowledge
    @PutMapping("/{id}/acknowledge")
    public ResponseEntity<NotificationDto.NotificationResponse> acknowledge(@PathVariable Long id) {
        return ResponseEntity.ok(notifService.acknowledge(id));
    }

    // DELETE /api/notifications/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<NotificationDto.MessageResponse> delete(@PathVariable Long id) {
        return ResponseEntity.ok(notifService.deleteNotification(id));
    }

    // GET /api/notifications/all  — admin: see all platform notifications
    @GetMapping("/all")
    public ResponseEntity<List<NotificationDto.NotificationResponse>> getAllAdmin() {
        return ResponseEntity.ok(notifService.getAll());
    }
}
