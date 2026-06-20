package com.exam.dto;

import java.util.List;

public class ProctorSummaryDTO {
    private Long examId;
    private String examTitle;
    private int onlineCount;
    private int submittedCount;
    private int anomalyCount;
    private List<ProctorStudentDTO> students;

    public Long getExamId() { return examId; }
    public void setExamId(Long examId) { this.examId = examId; }

    public String getExamTitle() { return examTitle; }
    public void setExamTitle(String examTitle) { this.examTitle = examTitle; }

    public int getOnlineCount() { return onlineCount; }
    public void setOnlineCount(int onlineCount) { this.onlineCount = onlineCount; }

    public int getSubmittedCount() { return submittedCount; }
    public void setSubmittedCount(int submittedCount) { this.submittedCount = submittedCount; }

    public int getAnomalyCount() { return anomalyCount; }
    public void setAnomalyCount(int anomalyCount) { this.anomalyCount = anomalyCount; }

    public List<ProctorStudentDTO> getStudents() { return students; }
    public void setStudents(List<ProctorStudentDTO> students) { this.students = students; }
}
