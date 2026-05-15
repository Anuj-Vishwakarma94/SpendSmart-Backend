package com.spendsmart.auth.dto;

import lombok.Data;
import java.util.List;

public class AdminDto {

    // ─── User list item (admin view) ──────────────────────
    @Data
    public static class UserListItem {
        private Long userId;
        private String fullName;
        private String email;
        private String role;
        private String provider;
        private Boolean isActive;
        private String currency;
        private String createdAt;
        private String updatedAt;
    }

    // ─── Platform stats ───────────────────────────────────
    @Data
    public static class PlatformStatsResponse {
        private long totalUsers;
        private long activeUsers;
        private long suspendedUsers;
        private long adminUsers;
        private long regularUsers;
    }

    // ─── Audit log entry ──────────────────────────────────
    @Data
    public static class AuditLogResponse {
        private Long id;
        private String actorEmail;
        private String action;
        private String targetDescription;
        private String createdAt;
    }

    // ─── Broadcast request ────────────────────────────────
    @Data
    public static class BroadcastRequest {
        private String message;
        // null = all users; provide list to target specific users
        private List<Long> userIds;
    }
}
