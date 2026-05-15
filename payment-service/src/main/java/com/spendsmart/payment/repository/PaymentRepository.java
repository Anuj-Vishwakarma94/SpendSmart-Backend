package com.spendsmart.payment.repository;

import com.spendsmart.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Payment> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, Payment.PaymentStatus status);

    List<Payment> findByExpenseId(Long expenseId);

    boolean existsByRazorpayOrderId(String razorpayOrderId);
}
