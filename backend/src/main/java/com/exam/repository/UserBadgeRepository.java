package com.exam.repository;

import com.exam.entity.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {
    List<UserBadge> findByUserIdOrderByEarnedAtDesc(Long userId);

    boolean existsByUserIdAndBadgeCode(Long userId, String badgeCode);

    long countByUserIdAndBadgeCode(Long userId, String badgeCode);
}
