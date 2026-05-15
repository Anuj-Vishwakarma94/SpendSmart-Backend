package com.spendsmart.subscription.repository;

import com.spendsmart.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByUserId(Long userId);

    Optional<Subscription> findByRazorpayOrderId(String razorpayOrderId);

    // Used by scheduler to auto-expire subscriptions
    List<Subscription> findByStatusAndEndDateBefore(
            Subscription.SubscriptionStatus status, LocalDate date);
}
