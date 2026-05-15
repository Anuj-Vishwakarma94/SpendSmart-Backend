package com.spendsmart.category.repository;

import com.spendsmart.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // ─── User-scoped ──────────────────────────────────────
    List<Category> findByUserId(Long userId);

    List<Category> findByUserIdAndType(Long userId, Category.CategoryType type);

    Optional<Category> findByUserIdAndName(Long userId, String name);

    Optional<Category> findByCategoryId(Long categoryId);

    void deleteByCategoryId(Long categoryId);

    void deleteByUserId(Long userId);

    long countByUserId(Long userId);

    // ─── Default (system) categories ─────────────────────
    List<Category> findByIsDefault(Boolean isDefault);

    List<Category> findByIsDefaultAndType(Boolean isDefault, Category.CategoryType type);

    // ─── Combined: user's own + defaults of the same type ─
    @Query("SELECT c FROM Category c WHERE (c.userId = :userId OR c.isDefault = true) " +
           "AND c.type = :type ORDER BY c.isDefault DESC, c.name ASC")
    List<Category> findAllForUserByType(@Param("userId") Long userId,
                                         @Param("type") Category.CategoryType type);

    @Query("SELECT c FROM Category c WHERE c.userId = :userId OR c.isDefault = true " +
           "ORDER BY c.isDefault DESC, c.name ASC")
    List<Category> findAllForUser(@Param("userId") Long userId);

    // ─── Existence checks ─────────────────────────────────
    boolean existsByUserIdAndNameAndType(Long userId, String name, Category.CategoryType type);
}
