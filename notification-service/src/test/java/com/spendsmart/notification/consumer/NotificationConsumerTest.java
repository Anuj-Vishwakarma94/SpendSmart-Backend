package com.spendsmart.notification.consumer;

import com.spendsmart.notification.dto.NotificationDto;
import com.spendsmart.notification.dto.NotificationMessage;
import com.spendsmart.notification.service.NotifService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock
    private NotifService notifService;

    @InjectMocks
    private NotificationConsumer notificationConsumer;

    @Test
    void consumeMessage_Success() {
        NotificationMessage msg = new NotificationMessage();
        msg.setRecipientId(1L);
        msg.setType("SYSTEM");
        msg.setSeverity("INFO");
        msg.setTitle("Test");
        msg.setMessage("Msg");

        when(notifService.send(any(NotificationDto.SendRequest.class))).thenReturn(new NotificationDto.NotificationResponse());

        notificationConsumer.consumeMessage(msg);

        verify(notifService, times(1)).send(any(NotificationDto.SendRequest.class));
    }

    @Test
    void consumeMessage_Exception() {
        NotificationMessage msg = new NotificationMessage();
        
        doThrow(new RuntimeException("DB error")).when(notifService).send(any(NotificationDto.SendRequest.class));

        // Should not throw exception, should just log error
        notificationConsumer.consumeMessage(msg);

        verify(notifService, times(1)).send(any(NotificationDto.SendRequest.class));
    }
}
