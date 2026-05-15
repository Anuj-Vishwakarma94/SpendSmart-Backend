package com.spendsmart.expense.repository;

import com.spendsmart.expense.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByUserIdOrderByDateDesc(Long userId);

    List<Expense> findByUserIdAndType(Long userId, Expense.ExpenseType type);

    List<Expense> findByUserIdAndCategoryId(Long userId, Long categoryId);

    List<Expense> findByUserIdAndDate(Long userId, LocalDate date);

    List<Expense> findByUserIdAndDateBetweenOrderByDateDesc(Long userId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT e FROM Expense e WHERE e.userId = :userId " +
           "AND MONTH(e.date) = :month AND YEAR(e.date) = :year ORDER BY e.date DESC")
    List<Expense> findByUserIdAndMonth(@Param("userId") Long userId,
                                       @Param("month") int month,
                                       @Param("year") int year);

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.userId = :userId")
    Double sumAmountByUserId(@Param("userId") Long userId);

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.userId = :userId AND e.categoryId = :categoryId")
    Double sumAmountByUserIdAndCategoryId(@Param("userId") Long userId, @Param("categoryId") Long categoryId);

    Optional<Expense> findByExpenseId(Long expenseId);

    void deleteByExpenseId(Long expenseId);

    @Query("SELECT e FROM Expense e WHERE e.userId = :userId " +
           "AND (LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(e.notes) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Expense> searchByKeyword(@Param("userId") Long userId, @Param("keyword") String keyword);

    @Query("SELECT e FROM Expense e WHERE e.userId = :userId " +
           "AND e.amount BETWEEN :minAmount AND :maxAmount ORDER BY e.date DESC")
    List<Expense> findByUserIdAndAmountBetween(@Param("userId") Long userId,
                                               @Param("minAmount") Double minAmount,
                                               @Param("maxAmount") Double maxAmount);

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.userId = :userId " +
           "AND MONTH(e.date) = :month AND YEAR(e.date) = :year")
    Double sumAmountByUserIdAndMonth(@Param("userId") Long userId,
                                     @Param("month") int month,
                                     @Param("year") int year);
}
