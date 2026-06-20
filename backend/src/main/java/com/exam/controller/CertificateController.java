package com.exam.controller;

import com.exam.service.CertificateService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/certificates")
public class CertificateController {

    private final CertificateService certificateService;

    public CertificateController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @GetMapping("/check/{examId}")
    public ResponseEntity<Map<String, Object>> checkEligibility(@PathVariable Long examId, Principal principal) {
        Map<String, Object> result = certificateService.checkEligibility(examId, principal.getName());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/download/{examId}/{submissionId}")
    public ResponseEntity<byte[]> downloadCertificate(@PathVariable Long examId, @PathVariable Long submissionId) {
        byte[] pdfData = certificateService.generateCertificatePdf(examId, submissionId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=certificate_EXAM-" + examId + "-" + submissionId + ".pdf")
                .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                .body(pdfData);
    }

    @GetMapping("/preview/{examId}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> previewCertificate(@PathVariable Long examId) {
        byte[] pdfData = certificateService.generatePreviewPdf(examId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=certificate_preview_EXAM-" + examId + ".pdf")
                .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                .body(pdfData);
    }
}
