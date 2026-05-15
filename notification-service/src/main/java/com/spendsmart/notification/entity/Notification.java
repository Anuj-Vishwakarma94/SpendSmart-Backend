package com.spendsmart.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    private Long recipientId;

    @Enumerated(EnumType.STRING)
    @Builder.Default private NotificationType type = NotificationType.SYSTEM;

    @Enumerated(EnumType.STRING)
    @Builder.Default private Severity severity = Severity.INFO;

    private String title;

    @Column(length = 1000)
    private String message;

    private Long relatedId;       // budgetId / recurringId etc.
    private String relatedType;   // "BUDGET" / "RECURRING" / "SYSTEM"

    @Builder.Default private Boolean isRead = false;
    @Builder.Default private Boolean isAcknowledged = false;

    @CreationTimestamp private LocalDateTime createdAt;

    public enum NotificationType {
        BUDGET_ALERT, BUDGET_EXCEEDED, RECURRING_DUE, MONTHLY_SUMMARY, SYSTEM
    }

    public enum Severity { INFO, WARNING, CRITICAL }
}
