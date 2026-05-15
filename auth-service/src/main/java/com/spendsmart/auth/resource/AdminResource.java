package com.spendsmart.auth.resource;

import com.spendsmart.auth.dto.AdminDto;
import com.spendsmart.auth.dto.AuthDto;
import com.spendsmart.auth.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminResource {

    private final AdminService adminService;

    // GET /api/admin/users
    @GetMapping("/users")
    public ResponseEntity<List<AdminDto.UserListItem>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    // PUT /api/admin/users/{id}/suspend
    @PutMapping("/users/{id}/suspend")
    public ResponseEntity<AuthDto.MessageResponse> suspendUser(
            @PathVariable Long id,
            Authentication auth) {
        adminService.logAction(auth.getName(), "SUSPEND_USER_REQUEST", "targetUserId=" + id);
        return ResponseEntity.ok(adminService.suspendUser(id));
    }

    // PUT /api/admin/users/{id}/unsuspend
    @PutMapping("/users/{id}/unsuspend")
    public ResponseEntity<AuthDto.MessageResponse> unsuspendUser(
            @PathVariable Long id,
            Authentication auth) {
        adminService.logAction(auth.getName(), "UNSUSPEND_USER_REQUEST", "targetUserId=" + id);
        return ResponseEntity.ok(adminService.unsuspendUser(id));
    }

    // DELETE /api/admin/users/{id}
    @DeleteMapping("/users/{id}")
    public ResponseEntity<AuthDto.MessageResponse> deleteUser(
            @PathVariable Long id,
            Authentication auth) {
        adminService.logAction(auth.getName(), "DELETE_USER_REQUEST", "targetUserId=" + id);
        return ResponseEntity.ok(adminService.deleteUser(id));
    }

    // GET /api/admin/analytics
    @GetMapping("/analytics")
    public ResponseEntity<AdminDto.PlatformStatsResponse> getPlatformStats() {
        return ResponseEntity.ok(adminService.getPlatformStats());
    }

    // POST /api/admin/notifications/broadcast
    @PostMapping("/notifications/broadcast")
    public ResponseEntity<AuthDto.MessageResponse> broadcast(
            @RequestBody AdminDto.BroadcastRequest request,
            Authentication auth) {
        adminService.logAction(auth.getName(), "BROADCAST_INITIATED", "msg=" + request.getMessage());
        return ResponseEntity.ok(adminService.broadcastNotification(request));
    }

    // GET /api/admin/audit-logs
    @GetMapping("/audit-logs")
    public ResponseEntity<List<AdminDto.AuditLogResponse>> getAuditLogs() {
        return ResponseEntity.ok(adminService.getAuditLogs());
    }
}
