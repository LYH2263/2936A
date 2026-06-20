package com.exam.controller;

import com.exam.entity.Appeal;
import com.exam.service.AppealService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appeals")
public class AppealController {

    private final AppealService appealService;

    public AppealController(AppealService appealService) {
        this.appealService = appealService;
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> submitAppeal(@RequestBody Map<String, Object> body, Principal principal) {
        try {
            Long submissionId = Long.valueOf(body.get("submissionId").toString());
            Long answerId = Long.valueOf(body.get("answerId").toString());
            String reason = (String) body.get("reason");

            Appeal appeal = appealService.submitAppeal(submissionId, answerId, reason, principal.getName());
            return ResponseEntity.ok(appeal);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public List<Appeal> getMyAppeals(Principal principal) {
        return appealService.getMyAppeals(principal.getName());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public List<Appeal> getPendingAppeals() {
        return appealService.getPendingAppeals();
    }

    @GetMapping("/pending-count")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public Map<String, Long> getPendingCount() {
        return Map.of("count", appealService.getPendingCount());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAppeal(@PathVariable Long id, Principal principal) {
        try {
            Appeal appeal = appealService.getAppealById(id);
            return ResponseEntity.ok(appeal);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/process")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<?> processAppeal(@PathVariable Long id, @RequestBody Map<String, Object> body, Principal principal) {
        try {
            String action = (String) body.get("action");
            String handlerComment = (String) body.get("handlerComment");
            Integer newScore = body.get("newScore") != null ? Integer.valueOf(body.get("newScore").toString()) : null;

            Appeal appeal = appealService.processAppeal(id, action, handlerComment, newScore, principal.getName());
            return ResponseEntity.ok(appeal);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
