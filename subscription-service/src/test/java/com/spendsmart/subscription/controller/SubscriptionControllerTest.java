package com.spendsmart.subscription.controller;

import com.spendsmart.subscription.dto.SubscriptionDto;
import com.spendsmart.subscription.service.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionControllerTest {

    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private SubscriptionController subscriptionController;

    private Long userId = 1L;

    private void mockAuth() {
        Authentication auth = mock(Authentication.class);
        when(auth.getDetails()).thenReturn(userId);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void getStatus() {
        mockAuth();
        when(subscriptionService.getStatus(userId)).thenReturn(new SubscriptionDto.SubscriptionStatusResponse());
        ResponseEntity<SubscriptionDto.SubscriptionStatusResponse> res = subscriptionController.getStatus();
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void initiateCheckout() {
        mockAuth();
        when(subscriptionService.initiateCheckout(userId)).thenReturn(new SubscriptionDto.CheckoutResponse());
        ResponseEntity<SubscriptionDto.CheckoutResponse> res = subscriptionController.initiateCheckout();
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void activate() {
        mockAuth();
        when(subscriptionService.activateSubscription(eq(userId), any())).thenReturn(new SubscriptionDto.SubscriptionStatusResponse());
        ResponseEntity<SubscriptionDto.SubscriptionStatusResponse> res = subscriptionController.activate(new SubscriptionDto.ActivateRequest());
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void cancel() {
        mockAuth();
        when(subscriptionService.cancelSubscription(userId)).thenReturn(new SubscriptionDto.MessageResponse("msg", true));
        ResponseEntity<SubscriptionDto.MessageResponse> res = subscriptionController.cancel();
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void isPremium() {
        mockAuth();
        when(subscriptionService.isPremium(userId)).thenReturn(true);
        ResponseEntity<Boolean> res = subscriptionController.isPremium();
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void getAllAdmin() {
        when(subscriptionService.getAllSubscriptions()).thenReturn(List.of(new SubscriptionDto.SubscriptionStatusResponse()));
        ResponseEntity<List<SubscriptionDto.SubscriptionStatusResponse>> res = subscriptionController.getAllAdmin();
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }
}
