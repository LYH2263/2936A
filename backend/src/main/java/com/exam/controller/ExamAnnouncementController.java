package com.exam.controller;

import com.exam.dto.ExamAnnouncementDTO;
import com.exam.service.ExamAnnouncementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exams")
public class ExamAnnouncementController {
    private final ExamAnnouncementService announcementService;

    public ExamAnnouncementController(ExamAnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping("/{examId}/announcements")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public List<Map<String, Object>> getAnnouncementsForTeacher(@PathVariable Long examId,
                                                                 Principal principal) {
        return announcementService.getAnnouncementsForTeacher(examId, principal.getName());
    }

    @PostMapping("/{examId}/announcements")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<?> createAnnouncement(@PathVariable Long examId,
                                                @RequestBody ExamAnnouncementDTO dto,
                                                Principal principal) {
        announcementService.createAnnouncement(examId, dto, principal.getName());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/announcements/{announcementId}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<?> updateAnnouncement(@PathVariable Long announcementId,
                                                @RequestBody ExamAnnouncementDTO dto,
                                                Principal principal) {
        announcementService.updateAnnouncement(announcementId, dto, principal.getName());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/announcements/{announcementId}/toggle-pin")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<?> togglePin(@PathVariable Long announcementId,
                                       Principal principal) {
        announcementService.togglePin(announcementId, principal.getName());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/announcements/{announcementId}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteAnnouncement(@PathVariable Long announcementId,
                                                Principal principal) {
        announcementService.deleteAnnouncement(announcementId, principal.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{examId}/announcements/student")
    public List<Map<String, Object>> getAnnouncementsForStudent(@PathVariable Long examId,
                                                                Principal principal) {
        return announcementService.getAnnouncementsForStudent(examId, principal.getName());
    }

    @GetMapping("/{examId}/announcements/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable Long examId,
                                                            Principal principal) {
        long count = announcementService.getUnreadCount(examId, principal.getName());
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PostMapping("/announcements/{announcementId}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long announcementId,
                                        Principal principal) {
        announcementService.markAsRead(announcementId, principal.getName());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{examId}/announcements/read-all")
    public ResponseEntity<?> markAllAsRead(@PathVariable Long examId,
                                           Principal principal) {
        announcementService.markAllAsRead(examId, principal.getName());
        return ResponseEntity.ok().build();
    }
}
