package com.exam.controller;

import com.exam.entity.QuestionFeedback;
import com.exam.entity.ExamQuestion;
import com.exam.repository.ExamQuestionRepository;
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

    public QuestionFeedbackController(QuestionFeedbackService feedbackService, ExamQuestionRepository examQuestionRepository) {
        this.feedbackService = feedbackService;
        this.examQuestionRepository = examQuestionRepository;
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> submitFeedback(@RequestBody Map<String, Object> body, Principal principal) {
        try {
            Long questionId = Long.valueOf(body.get("questionId").toString());
            String type = (String) body.get("type");
            String description = (String) body.get("description");

            QuestionFeedback feedback = feedbackService.submitFeedback(questionId, type, description, principal.getName());
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
    public ResponseEntity<?> getFeedback(@PathVariable Long id) {
        try {
            QuestionFeedback feedback = feedbackService.getFeedbackById(id);
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
