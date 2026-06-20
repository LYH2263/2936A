package com.exam.controller;

import com.exam.entity.FlashPracticeSession;
import com.exam.entity.Question;
import com.exam.service.FlashPracticeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    public FlashPracticeSession startSession(@RequestBody Map<String, Object> body, Principal principal) {
        String subject = (String) body.get("subject");
        Integer difficulty = body.containsKey("difficulty") && body.get("difficulty") != null
                ? Integer.valueOf(body.get("difficulty").toString())
                : null;
        String knowledgePoint = (String) body.get("knowledgePoint");
        return flashPracticeService.startSession(principal.getName(), subject, difficulty, knowledgePoint);
    }

    @GetMapping("/{sessionId}/next-question")
    public ResponseEntity<Question> getNextQuestion(@PathVariable Long sessionId) {
        try {
            Question q = flashPracticeService.getNextQuestion(sessionId);
            return ResponseEntity.ok(q);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{sessionId}/submit")
    public Map<String, Object> submitAnswer(@PathVariable Long sessionId, @RequestBody Map<String, Object> body) {
        Long questionId = Long.valueOf(body.get("questionId").toString());
        String answer = (String) body.get("answer");
        return flashPracticeService.submitAnswer(sessionId, questionId, answer);
    }

    @PostMapping("/{sessionId}/end")
    public ResponseEntity<?> endSession(@PathVariable Long sessionId) {
        flashPracticeService.endSession(sessionId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{sessionId}")
    public FlashPracticeSession getSession(@PathVariable Long sessionId) {
        return flashPracticeService.getSession(sessionId);
    }
}
