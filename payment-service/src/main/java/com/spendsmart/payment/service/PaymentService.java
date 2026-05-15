package com.spendsmart.payment.service;

import com.spendsmart.payment.dto.PaymentDto;

import java.util.List;

public interface PaymentService {

    /** Step 1: Create a Razorpay order and persist a CREATED record */
    PaymentDto.OrderResponse createOrder(Long userId, PaymentDto.CreateOrderRequest request);

    /** Step 2: Verify Razorpay signature and mark payment as PAID */
    PaymentDto.PaymentResponse verifyAndCapturePayment(Long userId, PaymentDto.VerifyPaymentRequest request);

    /** Initiate a refund (full or partial) */
    PaymentDto.RefundResponse initiateRefund(Long userId, PaymentDto.RefundRequest request);

    /** Get all payments for a user */
    List<PaymentDto.PaymentResponse> getPaymentsByUser(Long userId);

    /** Get a single payment by Razorpay order ID */
    PaymentDto.PaymentResponse getPaymentByOrderId(String razorpayOrderId);

    /** Get a single payment by internal DB ID */
    PaymentDto.PaymentResponse getPaymentById(Long id);
}
