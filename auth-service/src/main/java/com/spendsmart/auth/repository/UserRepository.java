package com.spendsmart.auth.repository;

import com.spendsmart.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByIsActive(Boolean isActive);

    List<User> findByCurrency(String currency);

    long countByIsActive(Boolean isActive);

    void deleteByUserId(Long userId);

    long countByRole(User.Role role);
}
