package com.exam.dto;

import java.time.LocalDateTime;

public class ProctorStudentDTO {
    private Long submissionId;
    private Long studentId;
    private String studentName;
    private String studentUsername;
    private String studentClazz;
    private double progress;
    private LocalDateTime startTime;
    private LocalDateTime lastActiveTime;
    private Integer tabSwitchCount;
    private String status;
    private String anomalyType;

    public Long getSubmissionId() { return submissionId; }
    public void setSubmissionId(Long submissionId) { this.submissionId = submissionId; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentUsername() { return studentUsername; }
    public void setStudentUsername(String studentUsername) { this.studentUsername = studentUsername; }

    public String getStudentClazz() { return studentClazz; }
    public void setStudentClazz(String studentClazz) { this.studentClazz = studentClazz; }

    public double getProgress() { return progress; }
    public void setProgress(double progress) { this.progress = progress; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getLastActiveTime() { return lastActiveTime; }
    public void setLastActiveTime(LocalDateTime lastActiveTime) { this.lastActiveTime = lastActiveTime; }

    public Integer getTabSwitchCount() { return tabSwitchCount; }
    public void setTabSwitchCount(Integer tabSwitchCount) { this.tabSwitchCount = tabSwitchCount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAnomalyType() { return anomalyType; }
    public void setAnomalyType(String anomalyType) { this.anomalyType = anomalyType; }
}
