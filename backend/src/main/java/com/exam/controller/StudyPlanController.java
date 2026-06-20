package com.exam.controller;

import com.exam.dto.StudyPlanDTO;
import com.exam.service.StudyPlanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/study-plans")
public class StudyPlanController {

    private final StudyPlanService studyPlanService;

    public StudyPlanController(StudyPlanService studyPlanService) {
        this.studyPlanService = studyPlanService;
    }

    @PostMapping
    public ResponseEntity<StudyPlanDTO> createPlan(Principal principal, @RequestBody StudyPlanDTO.CreateRequest request) {
        return ResponseEntity.ok(studyPlanService.createPlan(principal.getName(), request));
    }

    @GetMapping("/exam/{examId}")
    public ResponseEntity<StudyPlanDTO> getPlanByExam(Principal principal, @PathVariable Long examId) {
        StudyPlanDTO dto = studyPlanService.getPlanByExam(principal.getName(), examId);
        if (dto == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/my")
    public ResponseEntity<List<StudyPlanDTO>> getMyPlans(Principal principal) {
        return ResponseEntity.ok(studyPlanService.getMyPlans(principal.getName()));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<List<StudyPlanDTO.DashboardCard>> getDashboardCards(Principal principal) {
        return ResponseEntity.ok(studyPlanService.getDashboardCards(principal.getName()));
    }

    @PostMapping("/{planId}/check-in")
    public ResponseEntity<StudyPlanDTO> checkIn(Principal principal, @PathVariable Long planId, @RequestBody StudyPlanDTO.CheckInRequest request) {
        return ResponseEntity.ok(studyPlanService.checkIn(principal.getName(), planId, request));
    }

    @PostMapping("/{planId}/tasks")
    public ResponseEntity<StudyPlanDTO> addTask(Principal principal, @PathVariable Long planId, @RequestBody StudyPlanDTO.AddTaskRequest request) {
        return ResponseEntity.ok(studyPlanService.addTask(principal.getName(), planId, request));
    }

    @DeleteMapping("/{planId}/tasks/{taskId}")
    public ResponseEntity<?> deleteTask(Principal principal, @PathVariable Long planId, @PathVariable Long taskId) {
        studyPlanService.deleteTask(principal.getName(), planId, taskId);
        return ResponseEntity.ok().build();
    }
}
