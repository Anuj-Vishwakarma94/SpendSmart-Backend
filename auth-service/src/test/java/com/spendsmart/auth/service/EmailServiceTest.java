package com.spendsmart.auth.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@spendsmart.com");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "http://localhost:5173");
    }

    @Test
    void sendLoginNotification_Success() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        
        emailService.sendLoginNotification("test@example.com", "Test User");
        
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendLoginNotification_Exception_IsCaught() {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("Mail server down"));
        
        // This should not throw an exception as the catch block swallows it
        emailService.sendLoginNotification("test@example.com", "Test User");
        
        verify(mailSender, times(0)).send(any(MimeMessage.class));
    }

    @Test
    void sendPasswordResetEmail_Success() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        
        emailService.sendPasswordResetEmail("test@example.com", "Test User", "123456");
        
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendPasswordResetEmail_Exception_ThrowsRuntimeException() {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("Mail server down"));
        
        // This should throw a RuntimeException per the catch block
        assertThrows(RuntimeException.class, () -> 
            emailService.sendPasswordResetEmail("test@example.com", "Test User", "123456")
        );
    }
}
