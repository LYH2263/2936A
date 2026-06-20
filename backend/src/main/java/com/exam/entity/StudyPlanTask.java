package com.exam.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "study_plan_tasks")
public class StudyPlanTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "plan_id", nullable = false)
    private StudyPlan plan;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String completedDates;

    private Integer sortOrder;

    @PrePersist
    protected void onCreate() {
        if (sortOrder == null) sortOrder = 0;
        if (completedDates == null) completedDates = "";
    }

    public boolean isCompletedOn(String dateStr) {
        if (completedDates == null || completedDates.isEmpty()) return false;
        for (String d : completedDates.split(",")) {
            if (d.trim().equals(dateStr)) return true;
        }
        return false;
    }

    public void toggleDate(String dateStr, boolean completed) {
        java.util.Set<String> dates = new java.util.LinkedHashSet<>();
        if (completedDates != null && !completedDates.isEmpty()) {
            for (String d : completedDates.split(",")) {
                String trimmed = d.trim();
                if (!trimmed.isEmpty()) dates.add(trimmed);
            }
        }
        if (completed) {
            dates.add(dateStr);
        } else {
            dates.remove(dateStr);
        }
        completedDates = String.join(",", dates);
    }
}
