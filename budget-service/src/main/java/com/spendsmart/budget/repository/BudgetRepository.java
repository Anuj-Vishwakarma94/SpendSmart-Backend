package com.spendsmart.budget.repository;

import com.spendsmart.budget.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUserId(Long userId);
    Optional<Budget> findByBudgetId(Long budgetId);
    List<Budget> findByUserIdAndIsActive(Long userId, Boolean isActive);
    List<Budget> findByUserIdAndCategoryId(Long userId, Long categoryId);
    List<Budget> findByUserIdAndPeriod(Long userId, Budget.BudgetPeriod period);
    long countByUserId(Long userId);
    void deleteByBudgetId(Long budgetId);

    @Modifying
    @Query("UPDATE Budget b SET b.spentAmount = b.spentAmount + :delta WHERE b.budgetId = :id")
    void incrementSpent(@Param("id") Long budgetId, @Param("delta") Double delta);

    @Modifying
    @Query("UPDATE Budget b SET b.spentAmount = 0.0 WHERE b.userId = :userId AND b.period = :period")
    void resetPeriodForUser(@Param("userId") Long userId, @Param("period") Budget.BudgetPeriod period);
}
