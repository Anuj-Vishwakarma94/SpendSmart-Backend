package com.spendsmart.payment.controller;

import com.spendsmart.payment.dto.PaymentDto;
import com.spendsmart.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * POST /api/payments/create-order
     * Step 1: Frontend calls this to get a Razorpay order ID.
     * The response (especially keyId + razorpayOrderId) is used to open the Razorpay checkout.
     */
    @PostMapping("/create-order")
    public ResponseEntity<PaymentDto.OrderResponse> createOrder(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody PaymentDto.CreateOrderRequest request) {
        log.info("Create order request from userId: {}", userId);
        PaymentDto.OrderResponse response = paymentService.createOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/payments/verify
     * Step 2: After the user pays on Razorpay checkout, frontend sends the
     * payment result here for signature verification and capture.
     */
    @PostMapping("/verify")
    public ResponseEntity<PaymentDto.PaymentResponse> verifyPayment(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody PaymentDto.VerifyPaymentRequest request) {
        log.info("Verify payment request: orderId={}", request.getRazorpayOrderId());
        PaymentDto.PaymentResponse response = paymentService.verifyAndCapturePayment(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/payments/refund
     * Initiate a full or partial refund.
     */
    @PostMapping("/refund")
    public ResponseEntity<PaymentDto.RefundResponse> refundPayment(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody PaymentDto.RefundRequest request) {
        log.info("Refund request for paymentId: {}", request.getRazorpayPaymentId());
        PaymentDto.RefundResponse response = paymentService.initiateRefund(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/payments/my
     * Get all payments for the logged-in user.
     */
    @GetMapping("/my")
    public ResponseEntity<List<PaymentDto.PaymentResponse>> getMyPayments(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(paymentService.getPaymentsByUser(userId));
    }

    /**
     * GET /api/payments/{id}
     * Get a payment by internal DB ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentDto.PaymentResponse> getPaymentById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    /**
     * GET /api/payments/order/{orderId}
     * Get payment details by Razorpay order ID.
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentDto.PaymentResponse> getPaymentByOrderId(@PathVariable String orderId) {
        return ResponseEntity.ok(paymentService.getPaymentByOrderId(orderId));
    }
}
