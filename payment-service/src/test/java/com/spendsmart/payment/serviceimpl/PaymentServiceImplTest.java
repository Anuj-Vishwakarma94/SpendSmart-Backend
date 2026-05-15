package com.spendsmart.payment.serviceimpl;

import com.razorpay.Order;
import com.razorpay.OrderClient;
import com.razorpay.PaymentClient;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Refund;
import com.spendsmart.payment.dto.PaymentDto;
import com.spendsmart.payment.entity.Payment;
import com.spendsmart.payment.repository.PaymentRepository;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private RazorpayClient razorpayClient;

    @Mock
    private OrderClient orderClient;

    @Mock
    private PaymentClient paymentClient;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Payment payment;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "razorpayKeyId", "rzp_test_123");
        ReflectionTestUtils.setField(paymentService, "razorpayKeySecret", "secret123");
        ReflectionTestUtils.setField(paymentService, "defaultCurrency", "INR");

        razorpayClient.orders = orderClient;
        razorpayClient.payments = paymentClient;

        payment = Payment.builder()
                .id(1L)
                .userId(1L)
                .razorpayOrderId("order_123")
                .amount(500.0)
                .amountInPaise(50000L)
                .currency("INR")
                .status(Payment.PaymentStatus.CREATED)
                .build();
    }

    @Test
    void createOrder_Success() throws RazorpayException {
        PaymentDto.CreateOrderRequest req = new PaymentDto.CreateOrderRequest();
        req.setAmount(500.0);
        req.setCurrency("INR");
        req.setDescription("Test Payment");

        Order mockOrder = mock(Order.class);
        when(mockOrder.get("id")).thenReturn("order_123");
        
        when(orderClient.create(any(JSONObject.class))).thenReturn(mockOrder);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        PaymentDto.OrderResponse res = paymentService.createOrder(1L, req);

        assertNotNull(res);
        assertEquals("order_123", res.getRazorpayOrderId());
        assertEquals(500.0, res.getAmount());
    }

    @Test
    void createOrder_RazorpayException() throws RazorpayException {
        PaymentDto.CreateOrderRequest req = new PaymentDto.CreateOrderRequest();
        req.setAmount(500.0);

        when(orderClient.create(any(JSONObject.class))).thenThrow(new RazorpayException("Error"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> paymentService.createOrder(1L, req));
        assertTrue(ex.getMessage().contains("Failed to create payment order"));
    }

    @Test
    void verifyAndCapturePayment_Success() throws RazorpayException {
        PaymentDto.VerifyPaymentRequest req = new PaymentDto.VerifyPaymentRequest();
        req.setRazorpayOrderId("order_123");
        req.setRazorpayPaymentId("pay_123");
        
        // We must mock the verifySignature method. Since it's private and uses HMAC, we'll bypass it by reflecting a valid signature 
        // Wait, it's easier to generate a real HMAC-SHA256 signature for "order_123|pay_123" with secret "secret123"
        // For testing, let's just let it throw or handle the Exception if needed, or we just calculate it in the test.
        try {
            String payload = "order_123|pay_123";
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec("secret123".getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String generatedSignature = java.util.HexFormat.of().formatHex(hash);
            req.setRazorpaySignature(generatedSignature);
        } catch(Exception e) {
            // Ignore exception during test execution
        }

        when(paymentRepository.findByRazorpayOrderId("order_123")).thenReturn(Optional.of(payment));
        
        com.razorpay.Payment rzpPayment = mock(com.razorpay.Payment.class);
        when(rzpPayment.get("method")).thenReturn("upi");
        when(paymentClient.fetch("pay_123")).thenReturn(rzpPayment);
        
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        PaymentDto.PaymentResponse res = paymentService.verifyAndCapturePayment(1L, req);

        assertEquals(Payment.PaymentStatus.PAID, payment.getStatus());
        assertEquals(Payment.PaymentMethod.UPI, payment.getMethod());
        assertEquals("pay_123", res.getRazorpayPaymentId());
    }

    @Test
    void verifyAndCapturePayment_FailedSignature() {
        PaymentDto.VerifyPaymentRequest req = new PaymentDto.VerifyPaymentRequest();
        req.setRazorpayOrderId("order_123");
        req.setRazorpayPaymentId("pay_123");
        req.setRazorpaySignature("invalid_signature");

        when(paymentRepository.findByRazorpayOrderId("order_123")).thenReturn(Optional.of(payment));

        assertThrows(RuntimeException.class, () -> paymentService.verifyAndCapturePayment(1L, req));
        assertEquals(Payment.PaymentStatus.FAILED, payment.getStatus());
    }

    @Test
    void initiateRefund_Success() throws RazorpayException {
        PaymentDto.RefundRequest req = new PaymentDto.RefundRequest();
        req.setRazorpayPaymentId("pay_123");
        
        payment.setStatus(Payment.PaymentStatus.PAID);
        payment.setRazorpayPaymentId("pay_123");

        when(paymentRepository.findByRazorpayPaymentId("pay_123")).thenReturn(Optional.of(payment));
        
        Refund mockRefund = mock(Refund.class);
        when(mockRefund.get("id")).thenReturn("rfnd_123");
        when(paymentClient.refund(eq("pay_123"), any(JSONObject.class))).thenReturn(mockRefund);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        PaymentDto.RefundResponse res = paymentService.initiateRefund(1L, req);

        assertNotNull(res);
        assertEquals("rfnd_123", res.getRazorpayRefundId());
        assertEquals("processed", res.getStatus());
        assertEquals(Payment.PaymentStatus.REFUNDED, payment.getStatus());
    }

    @Test
    void initiateRefund_FullRefund() throws RazorpayException {
        PaymentDto.RefundRequest req = new PaymentDto.RefundRequest();
        req.setRazorpayPaymentId("pay_123");
        // no refund amount specified -> full refund
        
        payment.setStatus(Payment.PaymentStatus.PAID);
        payment.setAmountInPaise(50000L);
        payment.setRazorpayPaymentId("pay_123");

        when(paymentRepository.findByRazorpayPaymentId("pay_123")).thenReturn(Optional.of(payment));
        
        Refund mockRefund = mock(Refund.class);
        when(mockRefund.get("id")).thenReturn("rfnd_123");
        when(paymentClient.refund(eq("pay_123"), any(JSONObject.class))).thenReturn(mockRefund);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        PaymentDto.RefundResponse res = paymentService.initiateRefund(1L, req);

        assertNotNull(res);
        assertEquals(Payment.PaymentStatus.REFUNDED, payment.getStatus());
    }

    @Test
    void initiateRefund_NotPaid() {
        PaymentDto.RefundRequest req = new PaymentDto.RefundRequest();
        req.setRazorpayPaymentId("pay_123");
        
        payment.setStatus(Payment.PaymentStatus.FAILED);
        when(paymentRepository.findByRazorpayPaymentId("pay_123")).thenReturn(Optional.of(payment));

        assertThrows(RuntimeException.class, () -> paymentService.initiateRefund(1L, req));
    }

    @Test
    void initiateRefund_RazorpayException() throws RazorpayException {
        PaymentDto.RefundRequest req = new PaymentDto.RefundRequest();
        req.setRazorpayPaymentId("pay_123");
        
        payment.setStatus(Payment.PaymentStatus.PAID);
        when(paymentRepository.findByRazorpayPaymentId("pay_123")).thenReturn(Optional.of(payment));
        
        when(paymentClient.refund(eq("pay_123"), any(JSONObject.class))).thenThrow(new RazorpayException("API Error"));

        assertThrows(RuntimeException.class, () -> paymentService.initiateRefund(1L, req));
        assertEquals(Payment.RefundStatus.FAILED, payment.getRefundStatus());
    }

    @Test
    void getPaymentsByUser_Success() {
        when(paymentRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(payment));
        List<PaymentDto.PaymentResponse> res = paymentService.getPaymentsByUser(1L);
        assertEquals(1, res.size());
    }

    @Test
    void getPaymentByOrderId_Success() {
        when(paymentRepository.findByRazorpayOrderId("order_123")).thenReturn(Optional.of(payment));
        PaymentDto.PaymentResponse res = paymentService.getPaymentByOrderId("order_123");
        assertNotNull(res);
        assertEquals("order_123", res.getRazorpayOrderId());
    }

    @Test
    void getPaymentById_Success() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        PaymentDto.PaymentResponse res = paymentService.getPaymentById(1L);
        assertNotNull(res);
    }
}
