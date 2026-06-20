package com.exam.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "flash_practice_sessions", indexes = {
    @Index(name = "idx_flash_student", columnList = "student_id"),
    @Index(name = "idx_flash_student_date", columnList = "student_id, practice_date")
})
public class FlashPracticeSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(nullable = false)
    private String subject;

    private Integer difficulty;

    private String knowledgePoint;

    @Column(nullable = false)
    private LocalDate practiceDate;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Column(nullable = false)
    private Integer totalQuestions = 0;

    @Column(nullable = false)
    private Integer correctCount = 0;

    @Column(nullable = false)
    private Integer currentStreak = 0;

    @Column(nullable = false)
    private Integer maxStreak = 0;

    @Column(columnDefinition = "TEXT")
    private String answeredQuestionIds;

    public double getAccuracy() {
        if (totalQuestions == 0) return 0.0;
        return (double) correctCount / totalQuestions * 100;
    }
}
