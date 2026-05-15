package com.spendsmart.notification.consumer;

import com.spendsmart.notification.config.RabbitMQConfig;
import com.spendsmart.notification.dto.NotificationDto;
import com.spendsmart.notification.dto.NotificationMessage;
import com.spendsmart.notification.service.NotifService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotifService notifService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void consumeMessage(NotificationMessage message) {
        log.info("Received notification message from RabbitMQ: {}", message);
        try {
            NotificationDto.SendRequest req = new NotificationDto.SendRequest();
            req.setRecipientId(message.getRecipientId());
            req.setType(message.getType());
            req.setSeverity(message.getSeverity());
            req.setTitle(message.getTitle());
            req.setMessage(message.getMessage());
            req.setRelatedId(message.getRelatedId());
            req.setRelatedType(message.getRelatedType());
            
            notifService.send(req);
            log.info("Successfully processed and saved notification for user {}", message.getRecipientId());
        } catch (Exception e) {
            log.error("Failed to process notification message: {}", e.getMessage());
        }
    }
}
