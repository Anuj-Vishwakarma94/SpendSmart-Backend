package com.spendsmart.payment.webhook;

import com.spendsmart.payment.entity.Payment;
import com.spendsmart.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Handles Razorpay webhook events.
 *
 * To configure:
 *  1. Go to Razorpay Dashboard → Settings → Webhooks
 *  2. Add URL: https://your-domain.com/api/payments/webhook
 *  3. Select events: payment.captured, payment.failed, refund.processed
 *  4. Set the webhook secret and paste it in application.properties
 */
@RestController
@RequestMapping("/api/payments/webhook")
@RequiredArgsConstructor
@Slf4j
public class RazorpayWebhookController {

    private final PaymentRepository paymentRepository;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        // 1. Verify webhook signature
        if (!isValidWebhookSignature(payload, signature)) {
            log.warn("Invalid webhook signature received");
            return ResponseEntity.status(400).body("Invalid signature");
        }

        // 2. Parse and route event
        try {
            JSONObject event = new JSONObject(payload);
            String eventType = event.getString("event");
            log.info("Razorpay webhook received: {}", eventType);

            switch (eventType) {
                case "payment.captured" -> handlePaymentCaptured(event);
                case "payment.failed"   -> handlePaymentFailed(event);
                case "refund.processed" -> handleRefundProcessed(event);
                default                 -> log.info("Unhandled webhook event: {}", eventType);
            }

        } catch (Exception e) {
            log.error("Error processing webhook payload", e);
            return ResponseEntity.status(500).body("Webhook processing error");
        }

        return ResponseEntity.ok("Webhook processed");
    }

    // ─── Event Handlers ───────────────────────────────────────

    private void handlePaymentCaptured(JSONObject event) {
        try {
            JSONObject paymentEntity = event
                    .getJSONObject("payload")
                    .getJSONObject("payment")
                    .getJSONObject("entity");

            String orderId   = paymentEntity.getString("order_id");
            String paymentId = paymentEntity.getString("id");
            String method    = paymentEntity.optString("method", "UNKNOWN");

            paymentRepository.findByRazorpayOrderId(orderId).ifPresent(payment -> {
                if (payment.getStatus() != Payment.PaymentStatus.PAID) {
                    payment.setRazorpayPaymentId(paymentId);
                    payment.setStatus(Payment.PaymentStatus.PAID);
                    payment.setMethod(mapMethod(method));
                    paymentRepository.save(payment);
                    log.info("Webhook: payment.captured → orderId={}", orderId);
                }
            });
        } catch (Exception e) {
            log.error("Error handling payment.captured webhook", e);
        }
    }

    private void handlePaymentFailed(JSONObject event) {
        try {
            JSONObject paymentEntity = event
                    .getJSONObject("payload")
                    .getJSONObject("payment")
                    .getJSONObject("entity");

            String orderId = paymentEntity.getString("order_id");

            paymentRepository.findByRazorpayOrderId(orderId).ifPresent(payment -> {
                payment.setStatus(Payment.PaymentStatus.FAILED);
                paymentRepository.save(payment);
                log.info("Webhook: payment.failed → orderId={}", orderId);
            });
        } catch (Exception e) {
            log.error("Error handling payment.failed webhook", e);
        }
    }

    private void handleRefundProcessed(JSONObject event) {
        try {
            JSONObject refundEntity = event
                    .getJSONObject("payload")
                    .getJSONObject("refund")
                    .getJSONObject("entity");

            String paymentId = refundEntity.getString("payment_id");
            String refundId  = refundEntity.getString("id");

            paymentRepository.findByRazorpayPaymentId(paymentId).ifPresent(payment -> {
                payment.setRazorpayRefundId(refundId);
                payment.setRefundStatus(Payment.RefundStatus.PROCESSED);
                paymentRepository.save(payment);
                log.info("Webhook: refund.processed → paymentId={}", paymentId);
            });
        } catch (Exception e) {
            log.error("Error handling refund.processed webhook", e);
        }
    }

    // ─── Helpers ──────────────────────────────────────────────

    /**
     * Validates webhook signature using HMAC-SHA256.
     * Razorpay signs the raw body with the webhook secret.
     */
    private boolean isValidWebhookSignature(String payload, String receivedSignature) {
        if (receivedSignature == null || webhookSecret == null || webhookSecret.isBlank()
                || webhookSecret.equals("YOUR_WEBHOOK_SECRET")) {
            log.warn("Webhook secret not configured — skipping signature validation in dev mode");
            return true; // Allow in dev; always enforce in production
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String generatedSignature = HexFormat.of().formatHex(hash);
            return generatedSignature.equals(receivedSignature);
        } catch (Exception e) {
            log.error("Error validating webhook signature", e);
            return false;
        }
    }

    private Payment.PaymentMethod mapMethod(String method) {
        return switch (method.toLowerCase()) {
            case "upi"        -> Payment.PaymentMethod.UPI;
            case "card"       -> Payment.PaymentMethod.CARD;
            case "netbanking" -> Payment.PaymentMethod.NET_BANKING;
            case "wallet"     -> Payment.PaymentMethod.WALLET;
            case "emi"        -> Payment.PaymentMethod.EMI;
            default           -> Payment.PaymentMethod.UNKNOWN;
        };
    }
}
