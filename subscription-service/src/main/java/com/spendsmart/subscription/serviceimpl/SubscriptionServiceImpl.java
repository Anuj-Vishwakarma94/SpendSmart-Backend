package com.spendsmart.subscription.serviceimpl;

import com.spendsmart.subscription.config.PaymentServiceClient;
import com.spendsmart.subscription.dto.SubscriptionDto;
import com.spendsmart.subscription.entity.Subscription;
import com.spendsmart.subscription.repository.SubscriptionRepository;
import com.spendsmart.subscription.service.EmailService;
import com.spendsmart.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentServiceClient paymentServiceClient;
    private final EmailService emailService;

    @Value("${subscription.plan.monthly.price:199.00}")
    private Double monthlyPrice;

    @Value("${subscription.plan.monthly.currency:INR}")
    private String currency;

    // ─── Get Status ───────────────────────────────────────────

    @Override
    public SubscriptionDto.SubscriptionStatusResponse getStatus(Long userId) {
        Subscription sub = getOrCreateFreeSubscription(userId);
        return toStatusResponse(sub);
    }

    // ─── Initiate Checkout ────────────────────────────────────

    @Override
    @Transactional
    public SubscriptionDto.CheckoutResponse initiateCheckout(Long userId) {
        Subscription sub = getOrCreateFreeSubscription(userId);

        // Don't allow checkout if already active
        if (sub.isPremiumActive()) {
            throw new RuntimeException("You already have an active Premium subscription until " + sub.getEndDate());
        }

        // Call payment-service to create a Razorpay order
        SubscriptionDto.PaymentOrderRequest orderRequest = new SubscriptionDto.PaymentOrderRequest();
        orderRequest.setAmount(monthlyPrice);
        orderRequest.setCurrency(currency);
        orderRequest.setDescription("SpendSmart Premium - Monthly Plan");
        orderRequest.setNotes("Premium monthly subscription for userId=" + userId);

        SubscriptionDto.PaymentOrderResponse orderResponse =
                paymentServiceClient.createOrder(userId, orderRequest);

        // Mark subscription as PENDING
        sub.setStatus(Subscription.SubscriptionStatus.PENDING);
        sub.setRazorpayOrderId(orderResponse.getRazorpayOrderId());
        sub.setPlanType(Subscription.PlanType.PREMIUM_MONTHLY);
        subscriptionRepository.save(sub);

        log.info("Checkout initiated for userId={}, orderId={}", userId, orderResponse.getRazorpayOrderId());

        SubscriptionDto.CheckoutResponse response = new SubscriptionDto.CheckoutResponse();
        response.setRazorpayOrderId(orderResponse.getRazorpayOrderId());
        response.setAmount(monthlyPrice);
        response.setCurrency(currency);
        response.setKeyId(orderResponse.getKeyId());
        response.setSubscriptionDbId(sub.getId());
        response.setPlanType("PREMIUM_MONTHLY");
        response.setDescription("SpendSmart Premium - Monthly Plan (₹199/month)");
        return response;
    }

    // ─── Activate Subscription ────────────────────────────────

    @Override
    @Transactional
    public SubscriptionDto.SubscriptionStatusResponse activateSubscription(Long userId, SubscriptionDto.ActivateRequest request) {
        Subscription sub = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("No pending subscription found for userId=" + userId));

        if (sub.getStatus() != Subscription.SubscriptionStatus.PENDING) {
            throw new RuntimeException("Subscription is not in PENDING state");
        }

        // Delegate signature verification + payment capture to payment-service
        SubscriptionDto.PaymentVerifyRequest verifyRequest = new SubscriptionDto.PaymentVerifyRequest();
        verifyRequest.setRazorpayOrderId(request.getRazorpayOrderId());
        verifyRequest.setRazorpayPaymentId(request.getRazorpayPaymentId());
        verifyRequest.setRazorpaySignature(request.getRazorpaySignature());

        paymentServiceClient.verifyPayment(userId, verifyRequest); // throws on invalid signature

        // Activate: 30 days from today
        LocalDate today = LocalDate.now();
        sub.setStatus(Subscription.SubscriptionStatus.ACTIVE);
        sub.setStartDate(today);
        sub.setEndDate(today.plusDays(30));
        sub.setRazorpayPaymentId(request.getRazorpayPaymentId());
        sub.setAmountPaid(monthlyPrice);
        sub.setCurrency(currency);

        sub = subscriptionRepository.save(sub);
        log.info("Subscription ACTIVATED for userId={}, valid until {}", userId, sub.getEndDate());

        try {
            String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
            if (userEmail != null && !userEmail.isEmpty() && !userEmail.equals("anonymousUser")) {
                String amountStr = "₹" + monthlyPrice;
                emailService.sendPremiumWelcomeEmail(userEmail, amountStr);
            } else {
                log.warn("Could not find user email in SecurityContext to send premium welcome email");
            }
        } catch (Exception e) {
            log.error("Failed to trigger premium welcome email", e);
        }

        return toStatusResponse(sub);
    }

    // ─── Cancel ───────────────────────────────────────────────

    @Override
    @Transactional
    public SubscriptionDto.MessageResponse cancelSubscription(Long userId) {
        Subscription sub = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("No subscription found for userId=" + userId));

        if (sub.getStatus() != Subscription.SubscriptionStatus.ACTIVE) {
            throw new RuntimeException("No active subscription to cancel");
        }

        sub.setStatus(Subscription.SubscriptionStatus.CANCELLED);
        subscriptionRepository.save(sub);
        log.info("Subscription CANCELLED for userId={}", userId);

        return new SubscriptionDto.MessageResponse(
                "Subscription cancelled. You retain Premium access until " + sub.getEndDate(), true);
    }

    // ─── Scheduler: Auto-expire ───────────────────────────────

    @Override
    @Scheduled(cron = "0 0 0 * * *") // Runs every midnight
    @Transactional
    public void expireOverdueSubscriptions() {
        List<Subscription> expired = subscriptionRepository
                .findByStatusAndEndDateBefore(Subscription.SubscriptionStatus.ACTIVE, LocalDate.now());

        expired.forEach(sub -> {
            sub.setStatus(Subscription.SubscriptionStatus.EXPIRED);
            log.info("Subscription EXPIRED for userId={}", sub.getUserId());
        });

        subscriptionRepository.saveAll(expired);
        log.info("Expired {} subscriptions", expired.size());
    }

    // ─── isPremium check ─────────────────────────────────────

    @Override
    public boolean isPremium(Long userId) {
        return subscriptionRepository.findByUserId(userId)
                .map(Subscription::isPremiumActive)
                .orElse(false);
    }

    @Override
    public List<SubscriptionDto.SubscriptionStatusResponse> getAllSubscriptions() {
        return subscriptionRepository.findAll()
                .stream()
                .map(this::toStatusResponse)
                .toList();
    }

    // ─── Helpers ──────────────────────────────────────────────

    private Subscription getOrCreateFreeSubscription(Long userId) {
        return subscriptionRepository.findByUserId(userId)
                .orElseGet(() -> subscriptionRepository.save(
                        Subscription.builder().userId(userId).build()
                ));
    }

    private SubscriptionDto.SubscriptionStatusResponse toStatusResponse(Subscription sub) {
        SubscriptionDto.SubscriptionStatusResponse r = new SubscriptionDto.SubscriptionStatusResponse();
        r.setUserId(sub.getUserId());
        r.setPlanType(sub.getPlanType().name());
        r.setStatus(sub.getStatus().name());
        r.setPremium(sub.isPremiumActive());
        r.setStartDate(sub.getStartDate());
        r.setEndDate(sub.getEndDate());
        r.setAmountPaid(sub.getAmountPaid());
        r.setCurrency(sub.getCurrency());
        r.setUpdatedAt(sub.getUpdatedAt());

        if (sub.getEndDate() != null && sub.isPremiumActive()) {
            r.setDaysRemaining(ChronoUnit.DAYS.between(LocalDate.now(), sub.getEndDate()));
        } else {
            r.setDaysRemaining(0L);
        }
        return r;
    }
}
