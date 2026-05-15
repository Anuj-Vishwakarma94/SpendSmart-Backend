package com.spendsmart.subscription.serviceimpl;

import com.spendsmart.subscription.config.PaymentServiceClient;
import com.spendsmart.subscription.dto.SubscriptionDto;
import com.spendsmart.subscription.entity.Subscription;
import com.spendsmart.subscription.repository.SubscriptionRepository;
import com.spendsmart.subscription.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceImplTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PaymentServiceClient paymentServiceClient;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    private Subscription sub;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(subscriptionService, "monthlyPrice", 199.0);
        ReflectionTestUtils.setField(subscriptionService, "currency", "INR");

        sub = Subscription.builder()
                .id(1L)
                .userId(1L)
                .planType(Subscription.PlanType.FREE)
                .status(Subscription.SubscriptionStatus.FREE)
                .build();
    }

    @Test
    void getStatus_Success() {
        when(subscriptionRepository.findByUserId(1L)).thenReturn(Optional.of(sub));
        SubscriptionDto.SubscriptionStatusResponse res = subscriptionService.getStatus(1L);
        assertNotNull(res);
        assertEquals("FREE", res.getPlanType());
    }

    @Test
    void initiateCheckout_Success() {
        when(subscriptionRepository.findByUserId(1L)).thenReturn(Optional.of(sub));
        
        SubscriptionDto.PaymentOrderResponse orderRes = new SubscriptionDto.PaymentOrderResponse();
        orderRes.setRazorpayOrderId("order_123");
        orderRes.setKeyId("key_123");
        when(paymentServiceClient.createOrder(eq(1L), any())).thenReturn(orderRes);
        
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(sub);

        SubscriptionDto.CheckoutResponse res = subscriptionService.initiateCheckout(1L);
        assertNotNull(res);
        assertEquals("order_123", res.getRazorpayOrderId());
        assertEquals("key_123", res.getKeyId());
    }

    @Test
    void initiateCheckout_AlreadyActive() {
        sub.setStatus(Subscription.SubscriptionStatus.ACTIVE);
        sub.setEndDate(LocalDate.now().plusDays(10));
        when(subscriptionRepository.findByUserId(1L)).thenReturn(Optional.of(sub));

        assertThrows(RuntimeException.class, () -> subscriptionService.initiateCheckout(1L));
    }

    @Test
    void activateSubscription_Success() {
        sub.setStatus(Subscription.SubscriptionStatus.PENDING);
        when(subscriptionRepository.findByUserId(1L)).thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(sub);

        SubscriptionDto.ActivateRequest req = new SubscriptionDto.ActivateRequest();
        req.setRazorpayOrderId("order_123");
        req.setRazorpayPaymentId("pay_123");
        req.setRazorpaySignature("sig");

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("test@test.com");
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);

        SubscriptionDto.SubscriptionStatusResponse res = subscriptionService.activateSubscription(1L, req);
        assertNotNull(res);
        assertEquals(Subscription.SubscriptionStatus.ACTIVE, sub.getStatus());
        verify(paymentServiceClient, times(1)).verifyPayment(eq(1L), any());
        verify(emailService, times(1)).sendPremiumWelcomeEmail(eq("test@test.com"), anyString());
    }

    @Test
    void activateSubscription_NotPending() {
        when(subscriptionRepository.findByUserId(1L)).thenReturn(Optional.of(sub));

        SubscriptionDto.ActivateRequest req = new SubscriptionDto.ActivateRequest();
        assertThrows(RuntimeException.class, () -> subscriptionService.activateSubscription(1L, req));
    }

    @Test
    void cancelSubscription_Success() {
        sub.setStatus(Subscription.SubscriptionStatus.ACTIVE);
        when(subscriptionRepository.findByUserId(1L)).thenReturn(Optional.of(sub));

        SubscriptionDto.MessageResponse res = subscriptionService.cancelSubscription(1L);
        assertTrue(res.isSuccess());
        assertEquals(Subscription.SubscriptionStatus.CANCELLED, sub.getStatus());
        verify(subscriptionRepository, times(1)).save(sub);
    }

    @Test
    void cancelSubscription_NotActive() {
        when(subscriptionRepository.findByUserId(1L)).thenReturn(Optional.of(sub));
        assertThrows(RuntimeException.class, () -> subscriptionService.cancelSubscription(1L));
    }

    @Test
    void expireOverdueSubscriptions_Success() {
        when(subscriptionRepository.findByStatusAndEndDateBefore(eq(Subscription.SubscriptionStatus.ACTIVE), any(LocalDate.class)))
                .thenReturn(List.of(sub));

        subscriptionService.expireOverdueSubscriptions();
        assertEquals(Subscription.SubscriptionStatus.EXPIRED, sub.getStatus());
        verify(subscriptionRepository, times(1)).saveAll(anyList());
    }

    @Test
    void isPremium_Success() {
        sub.setStatus(Subscription.SubscriptionStatus.ACTIVE);
        sub.setEndDate(LocalDate.now().plusDays(10));
        when(subscriptionRepository.findByUserId(1L)).thenReturn(Optional.of(sub));

        assertTrue(subscriptionService.isPremium(1L));
    }

    @Test
    void getAllSubscriptions_Success() {
        when(subscriptionRepository.findAll()).thenReturn(List.of(sub));
        assertEquals(1, subscriptionService.getAllSubscriptions().size());
    }

    @Test
    void getStatus_PremiumActive() {
        sub.setPlanType(Subscription.PlanType.PREMIUM_MONTHLY);
        sub.setStatus(Subscription.SubscriptionStatus.ACTIVE);
        sub.setEndDate(LocalDate.now().plusDays(15));
        when(subscriptionRepository.findByUserId(1L)).thenReturn(Optional.of(sub));
        
        SubscriptionDto.SubscriptionStatusResponse res = subscriptionService.getStatus(1L);
        assertNotNull(res);
        assertEquals(15L, res.getDaysRemaining());
        assertTrue(res.isPremium());
    }

    @Test
    void getStatus_CreatesFreeSubscriptionIfNoneExists() {
        when(subscriptionRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(sub);
        
        SubscriptionDto.SubscriptionStatusResponse res = subscriptionService.getStatus(1L);
        assertNotNull(res);
        verify(subscriptionRepository, times(1)).save(any(Subscription.class));
    }

    @Test
    void activateSubscription_AnonymousUser() {
        sub.setStatus(Subscription.SubscriptionStatus.PENDING);
        when(subscriptionRepository.findByUserId(1L)).thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(sub);

        SubscriptionDto.ActivateRequest req = new SubscriptionDto.ActivateRequest();
        req.setRazorpayOrderId("order_123");
        req.setRazorpayPaymentId("pay_123");
        req.setRazorpaySignature("sig");

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("anonymousUser");
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);

        SubscriptionDto.SubscriptionStatusResponse res = subscriptionService.activateSubscription(1L, req);
        assertNotNull(res);
        verify(emailService, never()).sendPremiumWelcomeEmail(anyString(), anyString());
    }

    @Test
    void activateSubscription_EmailException() {
        sub.setStatus(Subscription.SubscriptionStatus.PENDING);
        when(subscriptionRepository.findByUserId(1L)).thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(sub);

        SubscriptionDto.ActivateRequest req = new SubscriptionDto.ActivateRequest();
        req.setRazorpayOrderId("order_123");
        req.setRazorpayPaymentId("pay_123");
        req.setRazorpaySignature("sig");

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("test@test.com");
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);

        doThrow(new RuntimeException("Email failed")).when(emailService).sendPremiumWelcomeEmail(anyString(), anyString());

        SubscriptionDto.SubscriptionStatusResponse res = subscriptionService.activateSubscription(1L, req);
        assertNotNull(res);
        verify(emailService, times(1)).sendPremiumWelcomeEmail(anyString(), anyString());
    }
}
