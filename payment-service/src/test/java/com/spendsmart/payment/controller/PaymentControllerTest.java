package com.spendsmart.payment.controller;

import com.spendsmart.payment.dto.PaymentDto;
import com.spendsmart.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private Long userId = 1L;

    @Test
    void createOrder_Success() {
        PaymentDto.OrderResponse res = new PaymentDto.OrderResponse();
        when(paymentService.createOrder(eq(userId), any())).thenReturn(res);

        ResponseEntity<PaymentDto.OrderResponse> response = paymentController.createOrder(userId, new PaymentDto.CreateOrderRequest());
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void verifyPayment_Success() {
        PaymentDto.PaymentResponse res = new PaymentDto.PaymentResponse();
        when(paymentService.verifyAndCapturePayment(eq(userId), any())).thenReturn(res);

        ResponseEntity<PaymentDto.PaymentResponse> response = paymentController.verifyPayment(userId, new PaymentDto.VerifyPaymentRequest());
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void refundPayment_Success() {
        PaymentDto.RefundResponse res = new PaymentDto.RefundResponse();
        when(paymentService.initiateRefund(eq(userId), any())).thenReturn(res);

        ResponseEntity<PaymentDto.RefundResponse> response = paymentController.refundPayment(userId, new PaymentDto.RefundRequest());
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getMyPayments_Success() {
        when(paymentService.getPaymentsByUser(userId)).thenReturn(List.of(new PaymentDto.PaymentResponse()));

        ResponseEntity<List<PaymentDto.PaymentResponse>> response = paymentController.getMyPayments(userId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getPaymentById_Success() {
        when(paymentService.getPaymentById(1L)).thenReturn(new PaymentDto.PaymentResponse());

        ResponseEntity<PaymentDto.PaymentResponse> response = paymentController.getPaymentById(1L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getPaymentByOrderId_Success() {
        when(paymentService.getPaymentByOrderId("order_123")).thenReturn(new PaymentDto.PaymentResponse());

        ResponseEntity<PaymentDto.PaymentResponse> response = paymentController.getPaymentByOrderId("order_123");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
