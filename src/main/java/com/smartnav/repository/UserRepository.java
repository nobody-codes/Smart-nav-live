package com.smartnav.repository;

import com.smartnav.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRole(String role);

    long countByCreatedAtAfter(LocalDateTime dateTime);

    List<User> findAllByOrderByPointsDesc();

    Optional<User> findByResetPasswordToken(String resetPasswordToken);

    List<User> findAllByOrderByCreatedAtDesc();
}
