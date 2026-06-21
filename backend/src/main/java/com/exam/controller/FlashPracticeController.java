package com.exam.controller;

import com.exam.entity.FlashPracticeSession;
import com.exam.entity.Question;
import com.exam.service.FlashPracticeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/flash-practice")
@PreAuthorize("hasRole('STUDENT')")
public class FlashPracticeController {

    private final FlashPracticeService flashPracticeService;

    public FlashPracticeController(FlashPracticeService flashPracticeService) {
        this.flashPracticeService = flashPracticeService;
    }

    @GetMapping("/subjects")
    public List<String> getSubjects() {
        return flashPracticeService.getAllSubjects();
    }

    @GetMapping("/knowledge-points")
    public List<String> getKnowledgePoints(@RequestParam(required = false) String subject) {
        return flashPracticeService.getKnowledgePointsBySubject(subject);
    }

    @GetMapping("/today-stats")
    public Map<String, Object> getTodayStats(Principal principal) {
        return flashPracticeService.getTodayStats(principal.getName());
    }

    @PostMapping("/start")
    public ResponseEntity<?> startSession(@RequestBody Map<String, Object> body, Principal principal) {
        String subject = (String) body.get("subject");
        if (subject == null || subject.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "请选择科目"));
        }
        Integer difficulty = null;
        if (body.containsKey("difficulty") && body.get("difficulty") != null) {
            try {
                difficulty = Integer.valueOf(body.get("difficulty").toString());
            } catch (NumberFormatException ignored) {
            }
        }
        String knowledgePoint = (String) body.get("knowledgePoint");
        FlashPracticeSession session = flashPracticeService.startSession(
                principal.getName(), subject, difficulty, knowledgePoint);
        return ResponseEntity.ok(session);
    }

    @GetMapping("/{sessionId}/next-question")
    public ResponseEntity<?> getNextQuestion(@PathVariable Long sessionId, Principal principal) {
        try {
            Question q = flashPracticeService.getNextQuestion(sessionId, principal.getName());
            return ResponseEntity.ok(q);
        } catch (SecurityException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{sessionId}/submit")
    public ResponseEntity<?> submitAnswer(@PathVariable Long sessionId,
                                           @RequestBody Map<String, Object> body,
                                           Principal principal) {
        try {
            Long questionId = Long.valueOf(body.get("questionId").toString());
            String answer = body.containsKey("answer") ? (String) body.get("answer") : null;
            Map<String, Object> result = flashPracticeService.submitAnswer(sessionId, questionId, answer, principal.getName());
            return ResponseEntity.ok(result);
        } catch (SecurityException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{sessionId}/end")
    public ResponseEntity<?> endSession(@PathVariable Long sessionId, Principal principal) {
        try {
            flashPracticeService.endSession(sessionId, principal.getName());
            return ResponseEntity.ok().build();
        } catch (SecurityException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        }
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<?> getSession(@PathVariable Long sessionId, Principal principal) {
        try {
            FlashPracticeSession s = flashPracticeService.getSession(sessionId, principal.getName());
            return ResponseEntity.ok(s);
        } catch (SecurityException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        }
    }
}
