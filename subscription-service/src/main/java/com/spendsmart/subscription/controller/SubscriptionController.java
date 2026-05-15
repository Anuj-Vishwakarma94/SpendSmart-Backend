package com.spendsmart.subscription.controller;

import com.spendsmart.subscription.dto.SubscriptionDto;
import com.spendsmart.subscription.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscription")
@RequiredArgsConstructor
@Slf4j
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    /**
     * GET /api/subscription/status
     * Returns the current user's plan status — FREE, ACTIVE, EXPIRED, etc.
     * Frontend uses this to show/hide premium features.
     */
    @GetMapping("/status")
    public ResponseEntity<SubscriptionDto.SubscriptionStatusResponse> getStatus() {
        Long userId = extractUserId();
        return ResponseEntity.ok(subscriptionService.getStatus(userId));
    }

    /**
     * POST /api/subscription/checkout
     * User clicks "Upgrade to Premium".
     * Returns razorpayOrderId + keyId needed to open Razorpay modal on frontend.
     */
    @PostMapping("/checkout")
    public ResponseEntity<SubscriptionDto.CheckoutResponse> initiateCheckout() {
        Long userId = extractUserId();
        return ResponseEntity.ok(subscriptionService.initiateCheckout(userId));
    }

    /**
     * POST /api/subscription/activate
     * Called by frontend after Razorpay payment succeeds.
     * Verifies payment and activates the 30-day Premium plan.
     */
    @PostMapping("/activate")
    public ResponseEntity<SubscriptionDto.SubscriptionStatusResponse> activate(
            @Valid @RequestBody SubscriptionDto.ActivateRequest request) {
        Long userId = extractUserId();
        return ResponseEntity.ok(subscriptionService.activateSubscription(userId, request));
    }

    /**
     * POST /api/subscription/cancel
     * Cancel subscription — Premium access continues until endDate.
     */
    @PostMapping("/cancel")
    public ResponseEntity<SubscriptionDto.MessageResponse> cancel() {
        Long userId = extractUserId();
        return ResponseEntity.ok(subscriptionService.cancelSubscription(userId));
    }

    /**
     * GET /api/subscription/is-premium
     * Lightweight boolean check for other services or frontend guards.
     */
    @GetMapping("/is-premium")
    public ResponseEntity<Boolean> isPremium() {
        Long userId = extractUserId();
        return ResponseEntity.ok(subscriptionService.isPremium(userId));
    }

    /**
     * GET /api/subscription/admin/all
     * Admin-only: returns all user subscriptions (for the Transactions dashboard).
     */
    @GetMapping("/admin/all")
    public ResponseEntity<List<SubscriptionDto.SubscriptionStatusResponse>> getAllAdmin() {
        return ResponseEntity.ok(subscriptionService.getAllSubscriptions());
    }

    // ─── Helper ───────────────────────────────────────────────

    private Long extractUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getDetails() == null) {
            throw new RuntimeException("Unauthorized");
        }
        return (Long) auth.getDetails();
    }
}
