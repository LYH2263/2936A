package com.exam.controller;

import com.exam.entity.QuestionFeedback;
import com.exam.entity.ExamQuestion;
import com.exam.entity.User;
import com.exam.repository.ExamQuestionRepository;
import com.exam.repository.UserRepository;
import com.exam.service.QuestionFeedbackService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/feedbacks")
public class QuestionFeedbackController {

    private final QuestionFeedbackService feedbackService;
    private final ExamQuestionRepository examQuestionRepository;
    private final UserRepository userRepository;

    public QuestionFeedbackController(QuestionFeedbackService feedbackService,
                                      ExamQuestionRepository examQuestionRepository,
                                      UserRepository userRepository) {
        this.feedbackService = feedbackService;
        this.examQuestionRepository = examQuestionRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> submitFeedback(@RequestBody Map<String, Object> body, Principal principal) {
        try {
            Long questionId = Long.valueOf(body.get("questionId").toString());
            Long submissionId = Long.valueOf(body.get("submissionId").toString());
            String type = (String) body.get("type");
            String description = (String) body.get("description");

            QuestionFeedback feedback = feedbackService.submitFeedback(questionId, submissionId, type, description, principal.getName());
            return ResponseEntity.ok(feedback);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public List<QuestionFeedback> getMyFeedbacks(Principal principal) {
        return feedbackService.getMyFeedbacks(principal.getName());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public List<QuestionFeedback> getPendingFeedbacks() {
        return feedbackService.getPendingFeedbacks();
    }

    @GetMapping("/pending-count")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Map<String, Long> getPendingCount() {
        return Map.of("count", feedbackService.getPendingCount());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getFeedback(@PathVariable Long id, Principal principal) {
        try {
            QuestionFeedback feedback = feedbackService.getFeedbackById(id);
            User currentUser = userRepository.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            boolean isOwner = feedback.getStudent().getId().equals(currentUser.getId());
            boolean isTeacher = "TEACHER".equals(currentUser.getRole()) || "ADMIN".equals(currentUser.getRole());

            if (!isOwner && !isTeacher) {
                return ResponseEntity.status(403).body(Map.of("message", "无权查看该工单"));
            }

            return ResponseEntity.ok(feedback);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/process")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<?> processFeedback(@PathVariable Long id, @RequestBody Map<String, Object> body, Principal principal) {
        try {
            String action = (String) body.get("action");
            String handlerComment = (String) body.get("handlerComment");

            QuestionFeedback feedback = feedbackService.processFeedback(id, action, handlerComment, principal.getName());
            return ResponseEntity.ok(feedback);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/question-exams/{questionId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<?> getExamsForQuestion(@PathVariable Long questionId) {
        List<ExamQuestion> eqs = examQuestionRepository.findByQuestionId(questionId);
        List<Map<String, Object>> result = eqs.stream().map(eq -> Map.<String, Object>of(
                "examId", eq.getExam().getId(),
                "examTitle", eq.getExam().getTitle()
        )).toList();
        return ResponseEntity.ok(result);
    }
}
