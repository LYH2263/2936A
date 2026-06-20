package com.exam.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_badges", uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_badge", columnNames = {"user_id", "badge_code"})
}, indexes = {
    @Index(name = "idx_ub_user", columnList = "user_id")
})
public class UserBadge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "badge_code", nullable = false, length = 30)
    private String badgeCode;

    private LocalDateTime earnedAt;

    @PrePersist
    protected void onCreate() {
        earnedAt = LocalDateTime.now();
    }
}
