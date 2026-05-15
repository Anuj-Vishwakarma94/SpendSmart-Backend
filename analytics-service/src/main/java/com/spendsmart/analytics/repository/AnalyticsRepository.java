package com.spendsmart.analytics.repository;

import com.spendsmart.analytics.entity.FinancialSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface AnalyticsRepository extends JpaRepository<FinancialSnapshot, Long> {
    List<FinancialSnapshot> findByUserId(Long userId);
    Optional<FinancialSnapshot> findByUserIdAndYearAndMonth(Long userId, int year, int month);
    List<FinancialSnapshot> findByUserIdAndYear(Long userId, int year);

    @Query("SELECT AVG(s.savingsRate) FROM FinancialSnapshot s WHERE s.userId = :userId")
    Double avgSavingsRateByUserId(@Param("userId") Long userId);

    @Query("SELECT s FROM FinancialSnapshot s WHERE s.userId = :userId ORDER BY s.year DESC, s.month DESC")
    List<FinancialSnapshot> findRecentByUserId(@Param("userId") Long userId);

    long countByUserId(Long userId);
}
