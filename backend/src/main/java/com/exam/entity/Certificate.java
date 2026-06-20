package com.exam.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "certificates", indexes = {
    @Index(name = "idx_cert_exam", columnList = "exam_id"),
    @Index(name = "idx_cert_submission", columnList = "submission_id"),
    @Index(name = "idx_cert_no", columnList = "certificate_no", unique = true)
})
public class Certificate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String certificateNo;

    @ManyToOne
    @JoinColumn(name = "exam_id")
    private Exam exam;

    @ManyToOne
    @JoinColumn(name = "submission_id")
    private Submission submission;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private User student;

    private String studentName;
    private String studentUsername;
    private String examTitle;
    private Integer score;
    private Integer totalScore;
    private String issuer;

    private LocalDateTime issueDate;

    private Integer downloadCount = 0;

    @PrePersist
    protected void onCreate() {
        if (issueDate == null) {
            issueDate = LocalDateTime.now();
        }
    }
}
