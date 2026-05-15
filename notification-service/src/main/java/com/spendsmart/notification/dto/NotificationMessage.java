package com.spendsmart.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationMessage {
    private Long recipientId;
    private String type;
    private String severity;
    private String title;
    private String message;
    private Long relatedId;
    private String relatedType;
}
