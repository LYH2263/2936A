package com.exam.controller;

import com.exam.service.ProctorService;
import com.exam.dto.ProctorSummaryDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/proctor")
public class ProctorController {
    
    private final ProctorService proctorService;

    public ProctorController(ProctorService proctorService) {
        this.proctorService = proctorService;
    }

    @GetMapping("/{examId}")
    public ResponseEntity<ProctorSummaryDTO> getProctorData(@PathVariable Long examId, Principal principal) {
        if (!proctorService.canAccessProctor(examId, principal.getName())) {
            throw new AccessDeniedException("无权访问此监考页面");
        }
        return ResponseEntity.ok(proctorService.getProctorData(examId));
    }

    @GetMapping("/{examId}/export")
    public ResponseEntity<byte[]> exportProctorSnapshot(@PathVariable Long examId, Principal principal) {
        if (!proctorService.canAccessProctor(examId, principal.getName())) {
            throw new AccessDeniedException("无权访问此监考页面");
        }
        
        byte[] data = proctorService.exportProctorSnapshot(examId);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=proctor_snapshot_" + examId + "_" + System.currentTimeMillis() + ".csv")
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(data);
    }
}
