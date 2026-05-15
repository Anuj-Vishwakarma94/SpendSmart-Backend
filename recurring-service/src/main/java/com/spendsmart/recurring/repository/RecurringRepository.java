package com.spendsmart.recurring.repository;

import com.spendsmart.recurring.entity.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.*;

@Repository
public interface RecurringRepository extends JpaRepository<RecurringTransaction, Long> {
    List<RecurringTransaction> findByUserId(Long userId);
    Optional<RecurringTransaction> findByRecurringId(Long recurringId);
    List<RecurringTransaction> findByUserIdAndIsActive(Long userId, Boolean isActive);
    List<RecurringTransaction> findByUserIdAndType(Long userId, RecurringTransaction.TransactionType type);

    /** Finds all active rules whose nextDueDate is today or earlier — used by the scheduler. */
    List<RecurringTransaction> findByIsActiveAndNextDueDateLessThanEqual(Boolean isActive, LocalDate date);

    /** Recurring rules due within the current month. */
    @Query("SELECT r FROM RecurringTransaction r WHERE r.userId = :uid AND r.isActive = true " +
           "AND MONTH(r.nextDueDate) = MONTH(CURRENT_DATE) AND YEAR(r.nextDueDate) = YEAR(CURRENT_DATE)")
    List<RecurringTransaction> findDueThisMonth(@Param("uid") Long userId);

    /** Rules due in the next :days days — used for reminder notifications. */
    @Query("SELECT r FROM RecurringTransaction r WHERE r.isActive = true " +
           "AND r.nextDueDate BETWEEN CURRENT_DATE AND :cutoff")
    List<RecurringTransaction> findDueWithinDays(@Param("cutoff") LocalDate cutoff);

    List<RecurringTransaction> findByFrequency(RecurringTransaction.Frequency frequency);
    long countByUserIdAndIsActive(Long userId, Boolean isActive);
    void deleteByRecurringId(Long recurringId);
}
