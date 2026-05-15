package com.spendsmart.payment.serviceimpl;

import com.razorpay.Order;
import com.razorpay.Refund;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.spendsmart.payment.dto.PaymentDto;
import com.spendsmart.payment.entity.Payment;
import com.spendsmart.payment.repository.PaymentRepository;
import com.spendsmart.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final RazorpayClient razorpayClient;
    private final PaymentRepository paymentRepository;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.currency:INR}")
    private String defaultCurrency;

    // ─── Create Order ─────────────────────────────────────────

    @Override
    @Transactional
    public PaymentDto.OrderResponse createOrder(Long userId, PaymentDto.CreateOrderRequest request) {
        try {
            String currency = request.getCurrency() != null ? request.getCurrency() : defaultCurrency;

            // Razorpay requires amount in paise (1 INR = 100 paise)
            long amountInPaise = Math.round(request.getAmount() * 100);

            // Build Razorpay order
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", currency);
            orderRequest.put("receipt", "rcpt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10));
            orderRequest.put("payment_capture", 1); // auto-capture

            if (request.getNotes() != null) {
                JSONObject notes = new JSONObject();
                notes.put("description", request.getNotes());
                notes.put("userId", userId.toString());
                orderRequest.put("notes", notes);
            }

            Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String razorpayOrderId = razorpayOrder.get("id");

            log.info("Razorpay order created: {} for userId: {}", razorpayOrderId, userId);

            // Persist to DB
            Payment payment = Payment.builder()
                    .userId(userId)
                    .razorpayOrderId(razorpayOrderId)
                    .amount(request.getAmount())
                    .amountInPaise(amountInPaise)
                    .currency(currency)
                    .description(request.getDescription())
                    .expenseId(request.getExpenseId())
                    .notes(request.getNotes())
                    .receiptId(orderRequest.getString("receipt"))
                    .status(Payment.PaymentStatus.CREATED)
                    .build();

            payment = paymentRepository.save(payment);

            // Build response
            PaymentDto.OrderResponse response = new PaymentDto.OrderResponse();
            response.setRazorpayOrderId(razorpayOrderId);
            response.setAmount(request.getAmount());
            response.setCurrency(currency);
            response.setStatus("created");
            response.setKeyId(razorpayKeyId);          // Send public key to frontend
            response.setDescription(request.getDescription());
            response.setPaymentDbId(payment.getId());

            return response;

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order for userId: {}", userId, e);
            throw new RuntimeException("Failed to create payment order: " + e.getMessage(), e);
        }
    }

    // ─── Verify & Capture Payment ─────────────────────────────

    @Override
    @Transactional
    public PaymentDto.PaymentResponse verifyAndCapturePayment(Long userId, PaymentDto.VerifyPaymentRequest request) {
        // 1. Find the payment record
        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new RuntimeException("Payment order not found: " + request.getRazorpayOrderId()));

        // 2. Verify HMAC-SHA256 signature
        boolean isValid = verifySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        if (!isValid) {
            log.warn("Invalid Razorpay signature for order: {}", request.getRazorpayOrderId());
            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new RuntimeException("Payment verification failed: invalid signature");
        }

        // 3. Mark as PAID
        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpaySignature(request.getRazorpaySignature());
        payment.setStatus(Payment.PaymentStatus.PAID);

        // 4. Optionally fetch payment method from Razorpay
        try {
            com.razorpay.Payment rzpPayment = razorpayClient.payments.fetch(request.getRazorpayPaymentId());
            String method = rzpPayment.get("method");
            payment.setMethod(mapPaymentMethod(method));
        } catch (RazorpayException e) {
            log.warn("Could not fetch payment method for: {}", request.getRazorpayPaymentId());
        }

        payment = paymentRepository.save(payment);
        log.info("Payment verified and captured: {} for userId: {}", request.getRazorpayPaymentId(), userId);

        return toPaymentResponse(payment);
    }

    // ─── Refund ───────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentDto.RefundResponse initiateRefund(Long userId, PaymentDto.RefundRequest request) {
        Payment payment = paymentRepository.findByRazorpayPaymentId(request.getRazorpayPaymentId())
                .orElseThrow(() -> new RuntimeException("Payment not found: " + request.getRazorpayPaymentId()));

        if (payment.getStatus() != Payment.PaymentStatus.PAID) {
            throw new RuntimeException("Cannot refund a payment that is not in PAID status");
        }

        try {
            JSONObject refundRequest = new JSONObject();

            // If no refundAmount provided, refund the full amount
            long refundPaise = (request.getRefundAmount() != null)
                    ? Math.round(request.getRefundAmount() * 100)
                    : payment.getAmountInPaise();

            refundRequest.put("amount", refundPaise);
            if (request.getReason() != null) {
                refundRequest.put("notes", new JSONObject().put("reason", request.getReason()));
            }

            Refund refund = razorpayClient.payments.refund(request.getRazorpayPaymentId(), refundRequest);
            String refundId = refund.get("id");

            log.info("Refund initiated: {} for payment: {}", refundId, request.getRazorpayPaymentId());

            // Update payment record
            payment.setRazorpayRefundId(refundId);
            payment.setRefundAmountInPaise(refundPaise);
            payment.setRefundStatus(Payment.RefundStatus.PROCESSED);

            boolean isFullRefund = refundPaise >= payment.getAmountInPaise();
            payment.setStatus(isFullRefund
                    ? Payment.PaymentStatus.REFUNDED
                    : Payment.PaymentStatus.PARTIALLY_REFUNDED);

            paymentRepository.save(payment);

            PaymentDto.RefundResponse response = new PaymentDto.RefundResponse();
            response.setRazorpayRefundId(refundId);
            response.setRazorpayPaymentId(request.getRazorpayPaymentId());
            response.setRefundAmount((double) refundPaise / 100);
            response.setStatus("processed");
            response.setMessage("Refund initiated successfully");

            return response;

        } catch (RazorpayException e) {
            log.error("Refund failed for payment: {}", request.getRazorpayPaymentId(), e);
            payment.setRefundStatus(Payment.RefundStatus.FAILED);
            paymentRepository.save(payment);
            throw new RuntimeException("Refund failed: " + e.getMessage(), e);
        }
    }

    // ─── Queries ──────────────────────────────────────────────

    @Override
    public List<PaymentDto.PaymentResponse> getPaymentsByUser(Long userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toPaymentResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PaymentDto.PaymentResponse getPaymentByOrderId(String razorpayOrderId) {
        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new RuntimeException("Payment not found for order: " + razorpayOrderId));
        return toPaymentResponse(payment);
    }

    @Override
    public PaymentDto.PaymentResponse getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + id));
        return toPaymentResponse(payment);
    }

    // ─── Helpers ──────────────────────────────────────────────

    /**
     * Verify Razorpay payment signature using HMAC-SHA256.
     * Signature = HMAC_SHA256(razorpay_order_id + "|" + razorpay_payment_id, key_secret)
     */
    private boolean verifySignature(String orderId, String paymentId, String receivedSignature) {
        try {
            String payload = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String generatedSignature = HexFormat.of().formatHex(hash);
            return generatedSignature.equals(receivedSignature);
        } catch (Exception e) {
            log.error("Error verifying Razorpay signature", e);
            return false;
        }
    }

    private Payment.PaymentMethod mapPaymentMethod(String method) {
        if (method == null) return Payment.PaymentMethod.UNKNOWN;
        return switch (method.toLowerCase()) {
            case "upi" -> Payment.PaymentMethod.UPI;
            case "card" -> Payment.PaymentMethod.CARD;
            case "netbanking" -> Payment.PaymentMethod.NET_BANKING;
            case "wallet" -> Payment.PaymentMethod.WALLET;
            case "emi" -> Payment.PaymentMethod.EMI;
            default -> Payment.PaymentMethod.UNKNOWN;
        };
    }

    private PaymentDto.PaymentResponse toPaymentResponse(Payment payment) {
        PaymentDto.PaymentResponse response = new PaymentDto.PaymentResponse();
        response.setId(payment.getId());
        response.setUserId(payment.getUserId());
        response.setRazorpayOrderId(payment.getRazorpayOrderId());
        response.setRazorpayPaymentId(payment.getRazorpayPaymentId());
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency());
        response.setStatus(payment.getStatus().name());
        response.setMethod(payment.getMethod() != null ? payment.getMethod().name() : null);
        response.setDescription(payment.getDescription());
        response.setExpenseId(payment.getExpenseId());
        response.setRefundStatus(payment.getRefundStatus() != null ? payment.getRefundStatus().name() : null);
        response.setCreatedAt(payment.getCreatedAt());
        response.setUpdatedAt(payment.getUpdatedAt());
        return response;
    }
}
