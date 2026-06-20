package com.exam.dto;

import lombok.Data;
import java.util.List;

@Data
public class StudyPlanDTO {
    private Long id;
    private Long examId;
    private String examTitle;
    private Integer dailyGoalMinutes;
    private String status;
    private String createdAt;
    private String archivedAt;
    private List<TaskItem> tasks;
    private Long daysUntilExam;
    private Double todayProgress;
    private Integer streakDays;
    private Boolean isArchived;

    @Data
    public static class TaskItem {
        private Long id;
        private String title;
        private Integer sortOrder;
        private Boolean completedToday;
        private String completedDates;
    }

    @Data
    public static class CreateRequest {
        private Long examId;
        private Integer dailyGoalMinutes;
        private List<String> taskTitles;
    }

    @Data
    public static class CheckInRequest {
        private Long taskId;
        private Boolean completed;
    }

    @Data
    public static class AddTaskRequest {
        private String title;
    }

    @Data
    public static class DashboardCard {
        private Long planId;
        private Long examId;
        private String examTitle;
        private Long daysUntilExam;
        private Double todayProgress;
        private Integer streakDays;
        private Integer totalTasksToday;
        private Integer completedTasksToday;
    }
}
