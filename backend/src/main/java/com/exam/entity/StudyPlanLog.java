package com.exam.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "study_plan_logs", uniqueConstraints = {
    @UniqueConstraint(name = "uk_plan_log_date", columnNames = {"plan_id", "log_date"})
})
public class StudyPlanLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "plan_id", nullable = false)
    private StudyPlan plan;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    private Integer studiedMinutes = 0;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        updatedAt = LocalDateTime.now();
        if (studiedMinutes == null) studiedMinutes = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
