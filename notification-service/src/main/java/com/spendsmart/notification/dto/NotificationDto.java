package com.spendsmart.notification.dto;

import lombok.Data;
import java.util.List;

public class NotificationDto {

    @Data public static class SendRequest {
        private Long recipientId;
        private String type;
        private String severity = "INFO";
        private String title;
        private String message;
        private Long relatedId;
        private String relatedType;
    }

    @Data public static class BudgetAlertRequest {
        private Long recipientId;
        private Long budgetId;
        private String budgetName;
        private Double spentAmount;
        private Double limitAmount;
        private Double percentageUsed;
        private Boolean exceeded;
    }

    @Data public static class BulkRequest {
        private List<Long> recipientIds;
        private String title;
        private String message;
        private String type = "SYSTEM";
        private String severity = "INFO";
    }

    @Data public static class NotificationResponse {
        private Long notificationId;
        private Long recipientId;
        private String type;
        private String severity;
        private String title;
        private String message;
        private Long relatedId;
        private String relatedType;
        private Boolean isRead;
        private Boolean isAcknowledged;
        private String createdAt;
    }

    @Data public static class MessageResponse {
        private String message; private boolean success;
        public MessageResponse(String m, boolean s) { this.message = m; this.success = s; }
    }

    @Data public static class UnreadCountResponse {
        private long count;
        public UnreadCountResponse(long c) { this.count = c; }
    }
}
