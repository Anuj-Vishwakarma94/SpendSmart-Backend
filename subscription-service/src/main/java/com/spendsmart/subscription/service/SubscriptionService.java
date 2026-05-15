package com.spendsmart.subscription.service;

import com.spendsmart.subscription.dto.SubscriptionDto;
import java.util.List;

public interface SubscriptionService {

    /** Get current subscription status for a user */
    SubscriptionDto.SubscriptionStatusResponse getStatus(Long userId);

    /**
     * Step 1 — User clicks "Upgrade to Premium".
     * Creates a Razorpay order via payment-service and returns checkout details.
     */
    SubscriptionDto.CheckoutResponse initiateCheckout(Long userId);

    /**
     * Step 2 — Frontend sends payment result after Razorpay checkout completes.
     * Verifies payment via payment-service and activates the subscription.
     */
    SubscriptionDto.SubscriptionStatusResponse activateSubscription(Long userId, SubscriptionDto.ActivateRequest request);

    /** Cancel an active subscription (no refund — access continues until endDate) */
    SubscriptionDto.MessageResponse cancelSubscription(Long userId);

    /** Called by scheduler — marks ACTIVE subscriptions as EXPIRED if endDate passed */
    void expireOverdueSubscriptions();

    /** Quick boolean check used by other services */
    boolean isPremium(Long userId);

    /** Admin-only: return all subscriptions across all users */
    List<SubscriptionDto.SubscriptionStatusResponse> getAllSubscriptions();
}
