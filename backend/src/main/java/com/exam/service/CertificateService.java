package com.exam.service;

import com.exam.entity.*;
import com.exam.repository.*;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final ExamRepository examRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final ExamQuestionRepository examQuestionRepository;

    public CertificateService(CertificateRepository certificateRepository, ExamRepository examRepository, SubmissionRepository submissionRepository, UserRepository userRepository, ExamQuestionRepository examQuestionRepository) {
        this.certificateRepository = certificateRepository;
        this.examRepository = examRepository;
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
        this.examQuestionRepository = examQuestionRepository;
    }

    public Map<String, Object> checkEligibility(Long examId, String username) {
        Exam exam = examRepository.findById(examId).orElseThrow(() -> new RuntimeException("Exam not found"));
        User student = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

        boolean certEnabled = Boolean.TRUE.equals(exam.getEnableCert());
        if (!certEnabled) {
            return Map.of("eligible", false, "reason", "该考试未启用合格证书");
        }

        java.util.List<Submission> subs = submissionRepository.findByExamIdAndStudentUsername(examId, username);
        Submission submission = subs.stream().filter(s -> "SUBMITTED".equals(s.getState())).findFirst().orElse(null);
        if (submission == null) {
            return Map.of("eligible", false, "reason", "尚未提交答卷");
        }

        Integer totalScore = examQuestionRepository.sumScoreByExamId(examId);
        int total = totalScore != null ? totalScore : 0;
        int passScore = exam.getCertPassScore() != null ? exam.getCertPassScore() : (int) (total * 0.6);
        int score = submission.getScore() != null ? submission.getScore() : 0;

        if (score < passScore) {
            return Map.of("eligible", false, "reason", "成绩未达到分数线（需" + passScore + "分，当前" + score + "分）", "passScore", passScore, "score", score);
        }

        return Map.of("eligible", true, "passScore", passScore, "score", score, "submissionId", submission.getId(), "totalScore", total);
    }

    @Transactional
    public Certificate getOrCreateCertificate(Long examId, Long submissionId) {
        return certificateRepository.findByExamIdAndSubmissionId(examId, submissionId)
                .orElseGet(() -> createCertificate(examId, submissionId));
    }

    private Certificate createCertificate(Long examId, Long submissionId) {
        Exam exam = examRepository.findById(examId).orElseThrow(() -> new RuntimeException("Exam not found"));
        Submission submission = submissionRepository.findById(submissionId).orElseThrow(() -> new RuntimeException("Submission not found"));
        User student = submission.getStudent();

        Integer totalScore = examQuestionRepository.sumScoreByExamId(examId);
        int total = totalScore != null ? totalScore : 0;
        int passScore = exam.getCertPassScore() != null ? exam.getCertPassScore() : (int) (total * 0.6);
        int score = submission.getScore() != null ? submission.getScore() : 0;

        if (score < passScore) {
            throw new RuntimeException("成绩未达到分数线，无法生成证书");
        }

        Certificate cert = new Certificate();
        cert.setCertificateNo("EXAM-" + examId + "-" + submissionId);
        cert.setExam(exam);
        cert.setSubmission(submission);
        cert.setStudent(student);
        cert.setStudentName(student.getFullName());
        cert.setStudentUsername(student.getUsername());
        cert.setExamTitle(exam.getCertTitle() != null ? exam.getCertTitle() : exam.getTitle());
        cert.setScore(score);
        cert.setTotalScore(total);
        cert.setIssuer(exam.getCertIssuer());
        cert.setIssueDate(LocalDateTime.now());

        return certificateRepository.save(cert);
    }

    public byte[] generateCertificatePdf(Long examId, Long submissionId) {
        Certificate cert = getOrCreateCertificate(examId, submissionId);
        cert.setDownloadCount(cert.getDownloadCount() + 1);
        certificateRepository.save(cert);

        return buildPdf(cert);
    }

    public byte[] generatePreviewPdf(Long examId) {
        Exam exam = examRepository.findById(examId).orElseThrow(() -> new RuntimeException("Exam not found"));

        Certificate preview = new Certificate();
        preview.setCertificateNo("EXAM-" + examId + "-PREVIEW");
        preview.setStudentName("张三");
        preview.setStudentUsername("STU001");
        preview.setExamTitle(exam.getCertTitle() != null ? exam.getCertTitle() : exam.getTitle());
        Integer totalScore = examQuestionRepository.sumScoreByExamId(examId);
        int total = totalScore != null ? totalScore : 100;
        int passScore = exam.getCertPassScore() != null ? exam.getCertPassScore() : (int) (total * 0.6);
        preview.setScore(Math.max(passScore, (int) (total * 0.8)));
        preview.setTotalScore(total);
        preview.setIssuer(exam.getCertIssuer());
        preview.setIssueDate(LocalDateTime.now());

        return buildPdf(preview);
    }

    private byte[] buildPdf(Certificate cert) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            Document document = new Document(PageSize.A4.rotate(), 60, 60, 60, 60);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 32, Font.BOLD, new Color(26, 54, 110));
            Font subtitleFont = new Font(Font.HELVETICA, 16, Font.NORMAL, new Color(80, 80, 80));
            Font labelFont = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(50, 50, 50));
            Font valueFont = new Font(Font.HELVETICA, 14, Font.NORMAL, new Color(30, 30, 30));
            Font certNoFont = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(150, 150, 150));
            Font sealFont = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(180, 40, 40));

            Rectangle pageSize = document.getPageSize();
            float centerX = pageSize.getWidth() / 2;

            PdfPTable borderTable = new PdfPTable(1);
            borderTable.setWidthPercentage(100);
            PdfPCell borderCell = new PdfPCell();
            borderCell.setBorder(Rectangle.BOX);
            borderCell.setBorderWidth(3);
            borderCell.setBorderColor(new Color(26, 54, 110));
            borderCell.setPadding(40);
            borderCell.setBorderWidthTop(3);
            borderCell.setBorderWidthBottom(3);
            borderCell.setBorderWidthLeft(3);
            borderCell.setBorderWidthRight(3);

            PdfPTable innerTable = new PdfPTable(1);
            innerTable.setWidthPercentage(100);

            PdfPCell titleCell = new PdfPCell(new Phrase("\u5408\u683C\u8BC1\u4E66", titleFont));
            titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            titleCell.setBorder(Rectangle.NO_BORDER);
            titleCell.setPaddingBottom(20);
            innerTable.addCell(titleCell);

            PdfPCell examTitleCell = new PdfPCell(new Phrase(cert.getExamTitle(), subtitleFont));
            examTitleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            examTitleCell.setBorder(Rectangle.NO_BORDER);
            examTitleCell.setPaddingBottom(30);
            innerTable.addCell(examTitleCell);

            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(80);
            infoTable.setWidths(new float[]{1, 2});
            infoTable.setHorizontalAlignment(Element.ALIGN_CENTER);

            addInfoRow(infoTable, "\u59D3\u540D\uFF1A", cert.getStudentName(), labelFont, valueFont);
            addInfoRow(infoTable, "\u5B66\u53F7\uFF1A", cert.getStudentUsername(), labelFont, valueFont);
            addInfoRow(infoTable, "\u8003\u8BD5\u540D\u79F0\uFF1A", cert.getExamTitle(), labelFont, valueFont);
            addInfoRow(infoTable, "\u8003\u8BD5\u6210\u7EE9\uFF1A", cert.getScore() + " / " + cert.getTotalScore(), labelFont, valueFont);

            PdfPCell infoCell = new PdfPCell(infoTable);
            infoCell.setBorder(Rectangle.NO_BORDER);
            infoCell.setPaddingBottom(30);
            innerTable.addCell(infoCell);

            PdfPCell certifyCell = new PdfPCell(new Phrase("\u7ECF\u8003\u6838\uFF0C\u8BE5\u540C\u5B66\u5DF2\u8FBE\u5230\u5408\u683C\u6807\u51C6\uFF0C\u7279\u53D1\u6B64\u8BC1\u3002", valueFont));
            certifyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            certifyCell.setBorder(Rectangle.NO_BORDER);
            certifyCell.setPaddingBottom(40);
            innerTable.addCell(certifyCell);

            PdfPTable bottomTable = new PdfPTable(2);
            bottomTable.setWidthPercentage(80);
            bottomTable.setHorizontalAlignment(Element.ALIGN_CENTER);

            PdfPCell issuerCell = new PdfPCell(new Phrase("\u53D1\u8BC1\u5355\u4F4D\uFF1A" + (cert.getIssuer() != null ? cert.getIssuer() : ""), valueFont));
            issuerCell.setBorder(Rectangle.NO_BORDER);
            issuerCell.setPaddingBottom(10);
            bottomTable.addCell(issuerCell);

            String dateStr = cert.getIssueDate() != null ? cert.getIssueDate().format(DateTimeFormatter.ofPattern("yyyy\u5E74MM\u6708dd\u65E5")) : "";
            PdfPCell dateCell = new PdfPCell(new Phrase("\u53D1\u8BC1\u65E5\u671F\uFF1A" + dateStr, valueFont));
            dateCell.setBorder(Rectangle.NO_BORDER);
            dateCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            dateCell.setPaddingBottom(10);
            bottomTable.addCell(dateCell);

            PdfPCell sealCell = new PdfPCell(new Phrase("\u25C6 \u5408\u683C\u5370\u7AE0 \u25C6", sealFont));
            sealCell.setBorder(Rectangle.NO_BORDER);
            sealCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            sealCell.setPaddingTop(10);
            bottomTable.addCell(sealCell);

            PdfPCell certNoCell = new PdfPCell(new Phrase("\u8BC1\u4E66\u7F16\u53F7\uFF1A" + cert.getCertificateNo(), certNoFont));
            certNoCell.setBorder(Rectangle.NO_BORDER);
            certNoCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            certNoCell.setPaddingTop(10);
            bottomTable.addCell(certNoCell);

            PdfPCell bottomCell = new PdfPCell(bottomTable);
            bottomCell.setBorder(Rectangle.NO_BORDER);
            innerTable.addCell(bottomCell);

            borderCell.addElement(innerTable);
            borderTable.addCell(borderCell);

            document.add(borderTable);
            document.close();
        } catch (Exception e) {
            throw new RuntimeException("PDF\u751F\u6210\u5931\u8D25", e);
        }
        return baos.toByteArray();
    }

    private void addInfoRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setPadding(8);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        valueCell.setPadding(8);
        table.addCell(valueCell);
    }
}
