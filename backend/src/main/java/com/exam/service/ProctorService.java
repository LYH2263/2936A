package com.exam.service;

import com.exam.entity.Exam;
import com.exam.entity.Submission;
import com.exam.entity.SubmissionAnswer;
import com.exam.entity.User;
import com.exam.repository.ExamRepository;
import com.exam.repository.ExamQuestionRepository;
import com.exam.repository.SubmissionAnswerRepository;
import com.exam.repository.SubmissionRepository;
import com.exam.repository.UserRepository;
import com.exam.dto.ProctorStudentDTO;
import com.exam.dto.ProctorSummaryDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProctorService {
    private final ExamRepository examRepository;
    private final SubmissionRepository submissionRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final UserRepository userRepository;
    private final SubmissionAnswerRepository submissionAnswerRepository;

    private static final long INACTIVITY_THRESHOLD_MINUTES = 5;

    public ProctorService(ExamRepository examRepository, SubmissionRepository submissionRepository, 
                          ExamQuestionRepository examQuestionRepository, UserRepository userRepository,
                          SubmissionAnswerRepository submissionAnswerRepository) {
        this.examRepository = examRepository;
        this.submissionRepository = submissionRepository;
        this.examQuestionRepository = examQuestionRepository;
        this.userRepository = userRepository;
        this.submissionAnswerRepository = submissionAnswerRepository;
    }

    public boolean canAccessProctor(Long examId, String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return false;
        
        if ("ADMIN".equals(user.getRole())) return true;
        
        Exam exam = examRepository.findById(examId).orElse(null);
        if (exam == null) return false;
        
        return exam.getCreator() != null && exam.getCreator().getId().equals(user.getId());
    }

    @Transactional(readOnly = true)
    public ProctorSummaryDTO getProctorData(Long examId) {
        Exam exam = examRepository.findById(examId).orElseThrow(() -> new RuntimeException("Exam not found"));
        List<Submission> inProgressSubmissions = submissionRepository.findByExamIdAndState(examId, "IN_PROGRESS");
        List<Submission> submittedSubmissions = submissionRepository.findByExamIdAndState(examId, "SUBMITTED");
        
        int totalQuestions = examQuestionRepository.countByExamId(examId);
        
        List<SubmissionAnswer> allAnswers = submissionAnswerRepository.findBySubmissionExamId(examId);
        Map<Long, List<SubmissionAnswer>> answersBySubmission = allAnswers.stream()
                .collect(Collectors.groupingBy(a -> a.getSubmission().getId()));
        
        List<ProctorStudentDTO> students = new ArrayList<>();
        int anomalyCount = 0;
        
        for (Submission submission : inProgressSubmissions) {
            List<SubmissionAnswer> submissionAnswers = answersBySubmission.getOrDefault(submission.getId(), Collections.emptyList());
            ProctorStudentDTO dto = convertToProctorDTO(submission, totalQuestions, exam, submissionAnswers);
            if (dto.getAnomalyType() != null) {
                anomalyCount++;
            }
            students.add(dto);
        }
        
        students.sort((a, b) -> {
            if (a.getAnomalyType() != null && b.getAnomalyType() == null) return -1;
            if (a.getAnomalyType() == null && b.getAnomalyType() != null) return 1;
            if (a.getLastActiveTime() != null && b.getLastActiveTime() != null) {
                return b.getLastActiveTime().compareTo(a.getLastActiveTime());
            }
            return 0;
        });
        
        ProctorSummaryDTO summary = new ProctorSummaryDTO();
        summary.setExamId(examId);
        summary.setExamTitle(exam.getTitle());
        summary.setOnlineCount(inProgressSubmissions.size());
        summary.setSubmittedCount(submittedSubmissions.size());
        summary.setAnomalyCount(anomalyCount);
        summary.setAllowTabSwitch(exam.getAllowTabSwitch());
        summary.setTabSwitchLimit(exam.getTabSwitchLimit());
        summary.setStudents(students);
        
        return summary;
    }

    private ProctorStudentDTO convertToProctorDTO(Submission submission, int totalQuestions, Exam exam, 
                                                  List<SubmissionAnswer> submissionAnswers) {
        ProctorStudentDTO dto = new ProctorStudentDTO();
        User student = submission.getStudent();
        
        dto.setSubmissionId(submission.getId());
        dto.setStudentId(student.getId());
        dto.setStudentName(student.getFullName() != null ? student.getFullName() : student.getUsername());
        dto.setStudentUsername(student.getUsername());
        dto.setStudentClazz(student.getClazz());
        dto.setStartTime(submission.getStartTime());
        dto.setLastActiveTime(submission.getLastActiveTime());
        dto.setTabSwitchCount(submission.getTabSwitchCount() != null ? submission.getTabSwitchCount() : 0);
        dto.setStatus(submission.getState());
        
        if (totalQuestions > 0 && submissionAnswers != null) {
            long answeredCount = submissionAnswers.stream()
                    .filter(a -> a.getStudentAnswer() != null && !a.getStudentAnswer().isEmpty())
                    .count();
            dto.setProgress(Math.round((double) answeredCount / totalQuestions * 1000.0) / 10.0);
        } else {
            dto.setProgress(0.0);
        }
        
        String anomalyType = detectAnomaly(submission, exam);
        dto.setAnomalyType(anomalyType);
        
        return dto;
    }

    private String detectAnomaly(Submission submission, Exam exam) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastActive = submission.getLastActiveTime();
        
        if (lastActive != null) {
            long minutesInactive = Duration.between(lastActive, now).toMinutes();
            if (minutesInactive >= INACTIVITY_THRESHOLD_MINUTES) {
                return "INACTIVITY";
            }
        }
        
        if (exam != null && Boolean.FALSE.equals(exam.getAllowTabSwitch())) {
            Integer tabLimit = exam.getTabSwitchLimit();
            Integer tabCount = submission.getTabSwitchCount();
            if (tabLimit != null && tabCount != null && tabCount >= tabLimit) {
                return "EXCESS_TAB_SWITCH";
            }
        }
        
        return null;
    }

    public byte[] exportProctorSnapshot(Long examId) {
        ProctorSummaryDTO data = getProctorData(examId);
        StringBuilder sb = new StringBuilder();
        sb.append("\uFEFF");
        
        sb.append("考试名称,").append(data.getExamTitle()).append("\n");
        sb.append("导出时间,").append(LocalDateTime.now()).append("\n");
        sb.append("在线人数,").append(data.getOnlineCount()).append("\n");
        sb.append("已交卷人数,").append(data.getSubmittedCount()).append("\n");
        sb.append("异常人数,").append(data.getAnomalyCount()).append("\n\n");
        
        sb.append("学生列表\n");
        sb.append("学号,姓名,班级,答题进度(%),最近活跃时间,切屏次数,状态,异常类型\n");
        
        for (ProctorStudentDTO s : data.getStudents()) {
            String anomaly = s.getAnomalyType() != null ? 
                ("INACTIVITY".equals(s.getAnomalyType()) ? "长时间无操作" : "切屏超标") : "正常";
            
            sb.append(s.getStudentUsername()).append(",")
              .append(s.getStudentName()).append(",")
              .append(s.getStudentClazz() != null ? s.getStudentClazz() : "").append(",")
              .append(String.format("%.1f", s.getProgress())).append(",")
              .append(s.getLastActiveTime() != null ? s.getLastActiveTime().toString() : "").append(",")
              .append(s.getTabSwitchCount()).append(",")
              .append("IN_PROGRESS".equals(s.getStatus()) ? "在线" : s.getStatus()).append(",")
              .append(anomaly).append("\n");
        }
        
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
