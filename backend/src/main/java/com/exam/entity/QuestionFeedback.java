package com.exam.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "question_feedbacks", indexes = {
    @Index(name = "idx_fb_student", columnList = "student_id"),
    @Index(name = "idx_fb_question", columnList = "question_id"),
    @Index(name = "idx_fb_status", columnList = "status")
})
public class QuestionFeedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(nullable = false, length = 30)
    private String type;

    @Column(columnDefinition = "TEXT", length = 500)
    private String description;

    @Column(nullable = false, length = 20)
    private String status;

    @ManyToOne
    @JoinColumn(name = "handler_id")
    private User handler;

    @Column(columnDefinition = "TEXT")
    private String handlerComment;

    private LocalDateTime createdAt;

    private LocalDateTime handledAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = "PENDING";
        }
    }
}
