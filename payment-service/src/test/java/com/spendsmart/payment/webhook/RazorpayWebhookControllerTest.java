package com.spendsmart.payment.webhook;

import com.spendsmart.payment.entity.Payment;
import com.spendsmart.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RazorpayWebhookControllerTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private RazorpayWebhookController webhookController;

    private Payment payment;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(webhookController, "webhookSecret", "test_secret");

        payment = new Payment();
        payment.setId(1L);
        payment.setRazorpayOrderId("order_123");
        payment.setStatus(Payment.PaymentStatus.CREATED);
    }

    @Test
    void handleWebhook_InvalidSignature() {
        String payload = "{\"event\":\"payment.captured\"}";
        ResponseEntity<String> response = webhookController.handleWebhook(payload, "invalid_sig");
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void handleWebhook_NoSecretConfigured() {
        ReflectionTestUtils.setField(webhookController, "webhookSecret", "YOUR_WEBHOOK_SECRET");
        String payload = "{\"event\":\"unknown.event\"}";
        
        ResponseEntity<String> response = webhookController.handleWebhook(payload, "any_sig");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void handlePaymentCaptured_Success() {
        ReflectionTestUtils.setField(webhookController, "webhookSecret", "YOUR_WEBHOOK_SECRET");
        String payload = """
                {
                  "event": "payment.captured",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "pay_123",
                        "order_id": "order_123",
                        "method": "upi"
                      }
                    }
                  }
                }""";

        when(paymentRepository.findByRazorpayOrderId("order_123")).thenReturn(Optional.of(payment));

        ResponseEntity<String> response = webhookController.handleWebhook(payload, "sig");
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Payment.PaymentStatus.PAID, payment.getStatus());
        verify(paymentRepository, times(1)).save(payment);
    }

    @Test
    void handlePaymentFailed_Success() {
        ReflectionTestUtils.setField(webhookController, "webhookSecret", "YOUR_WEBHOOK_SECRET");
        String payload = """
                {
                  "event": "payment.failed",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "pay_123",
                        "order_id": "order_123"
                      }
                    }
                  }
                }""";

        when(paymentRepository.findByRazorpayOrderId("order_123")).thenReturn(Optional.of(payment));

        ResponseEntity<String> response = webhookController.handleWebhook(payload, "sig");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Payment.PaymentStatus.FAILED, payment.getStatus());
        verify(paymentRepository, times(1)).save(payment);
    }

    @Test
    void handleRefundProcessed_Success() {
        ReflectionTestUtils.setField(webhookController, "webhookSecret", "YOUR_WEBHOOK_SECRET");
        String payload = """
                {
                  "event": "refund.processed",
                  "payload": {
                    "refund": {
                      "entity": {
                        "id": "rfnd_123",
                        "payment_id": "pay_123"
                      }
                    }
                  }
                }""";

        payment.setRazorpayPaymentId("pay_123");
        when(paymentRepository.findByRazorpayPaymentId("pay_123")).thenReturn(Optional.of(payment));

        ResponseEntity<String> response = webhookController.handleWebhook(payload, "sig");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Payment.RefundStatus.PROCESSED, payment.getRefundStatus());
        verify(paymentRepository, times(1)).save(payment);
    }

    @Test
    void handleWebhook_Exception() {
        ReflectionTestUtils.setField(webhookController, "webhookSecret", "YOUR_WEBHOOK_SECRET");
        String payload = "invalid_json";

        ResponseEntity<String> response = webhookController.handleWebhook(payload, "sig");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}
