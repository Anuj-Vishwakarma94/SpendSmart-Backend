package com.spendsmart.income.repository;

import com.spendsmart.income.entity.Income;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface IncomeRepository extends JpaRepository<Income, Long> {

    // ─── Basic Queries ────────────────────────────────────
    List<Income> findByUserIdOrderByDateDesc(Long userId);

    Optional<Income> findByIncomeId(Long incomeId);

    void deleteByIncomeId(Long incomeId);

    // ─── Source Filter ────────────────────────────────────
    List<Income> findByUserIdAndSource(Long userId, Income.IncomeSource source);

    // ─── Date Queries ─────────────────────────────────────
    List<Income> findByUserIdAndDateBetweenOrderByDateDesc(Long userId,
                                                           LocalDate startDate,
                                                           LocalDate endDate);

    @Query("SELECT i FROM Income i WHERE i.userId = :userId " +
           "AND MONTH(i.date) = :month AND YEAR(i.date) = :year " +
           "ORDER BY i.date DESC")
    List<Income> findByUserIdAndMonth(@Param("userId") Long userId,
                                      @Param("month") int month,
                                      @Param("year") int year);

    // ─── Recurring ────────────────────────────────────────
    List<Income> findByUserIdAndIsRecurring(Long userId, Boolean isRecurring);

    List<Income> findByIsRecurring(Boolean isRecurring);

    // ─── Aggregations ─────────────────────────────────────
    @Query("SELECT SUM(i.amount) FROM Income i WHERE i.userId = :userId")
    Double sumAmountByUserId(@Param("userId") Long userId);

    @Query("SELECT SUM(i.amount) FROM Income i WHERE i.userId = :userId " +
           "AND MONTH(i.date) = :month AND YEAR(i.date) = :year")
    Double sumAmountByUserIdAndMonth(@Param("userId") Long userId,
                                     @Param("month") int month,
                                     @Param("year") int year);

    @Query("SELECT SUM(i.amount) FROM Income i WHERE i.userId = :userId " +
           "AND i.source = :source")
    Double sumAmountByUserIdAndSource(@Param("userId") Long userId,
                                      @Param("source") Income.IncomeSource source);

    // ─── Search ───────────────────────────────────────────
    @Query("SELECT i FROM Income i WHERE i.userId = :userId " +
           "AND (LOWER(i.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(i.notes) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Income> searchByKeyword(@Param("userId") Long userId,
                                  @Param("keyword") String keyword);

    // ─── Category ─────────────────────────────────────────
    List<Income> findByUserIdAndCategoryId(Long userId, Long categoryId);

    // ─── Counts ───────────────────────────────────────────
    long countByUserId(Long userId);
}
