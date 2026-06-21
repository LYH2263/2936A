package com.exam.service;

import com.exam.entity.*;
import com.exam.repository.*;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
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

    private BaseFont baseFont = null;

    public CertificateService(CertificateRepository certificateRepository, ExamRepository examRepository, SubmissionRepository submissionRepository, UserRepository userRepository, ExamQuestionRepository examQuestionRepository) {
        this.certificateRepository = certificateRepository;
        this.examRepository = examRepository;
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
        this.examQuestionRepository = examQuestionRepository;
        initFont();
    }

    private void initFont() {
        String[] fontCandidates = {
            "C:/Windows/Fonts/msyh.ttc,0",
            "C:/Windows/Fonts/simhei.ttf",
            "C:/Windows/Fonts/simsun.ttc,0",
            "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc",
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
            "/System/Library/Fonts/PingFang.ttc,0"
        };
        for (String path : fontCandidates) {
            try {
                File f = new File(path.split(",")[0]);
                if (f.exists()) {
                    baseFont = BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                    return;
                }
            } catch (Exception ignored) {}
        }
        try {
            baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED);
        } catch (Exception e) {
            throw new RuntimeException("无法初始化 PDF 字体", e);
        }
    }

    private Font createFont(float size, int style, Color color) {
        return new Font(baseFont, size, style, color);
    }

    public Map<String, Object> checkEligibility(Long examId, String username) {
        Exam exam = examRepository.findById(examId).orElseThrow(() -> new RuntimeException("Exam not found"));
        User student = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

        boolean certEnabled = Boolean.TRUE.equals(exam.getEnableCert());
        if (!certEnabled) {
            return Map.of("eligible", false, "reason", "该考试未启用合格证书", "enableCert", false);
        }

        java.util.List<Submission> subs = submissionRepository.findByExamIdAndStudentUsername(examId, username);
        Submission submission = subs.stream().filter(s -> "SUBMITTED".equals(s.getState())).findFirst().orElse(null);
        if (submission == null) {
            return Map.of("eligible", false, "reason", "尚未提交答卷", "enableCert", true);
        }

        Integer totalScore = examQuestionRepository.sumScoreByExamId(examId);
        int total = totalScore != null ? totalScore : 0;
        int passScore = exam.getCertPassScore() != null ? exam.getCertPassScore() : (int) (total * 0.6);
        int score = submission.getScore() != null ? submission.getScore() : 0;

        if (score < passScore) {
            return Map.of("eligible", false, "reason", "成绩未达到分数线（需" + passScore + "分，当前" + score + "分）",
                    "passScore", passScore, "score", score, "enableCert", true);
        }

        String certNo = "EXAM-" + examId + "-" + submission.getId();
        return Map.of("eligible", true, "passScore", passScore, "score", score,
                "submissionId", submission.getId(), "totalScore", total,
                "certificateNo", certNo, "enableCert", true);
    }

    @Transactional
    public Certificate getOrCreateCertificate(Long examId, Long submissionId, String username) {
        Exam exam = examRepository.findById(examId).orElseThrow(() -> new RuntimeException("Exam not found"));
        if (!Boolean.TRUE.equals(exam.getEnableCert())) {
            throw new RuntimeException("该考试未启用合格证书");
        }

        Submission submission = submissionRepository.findById(submissionId).orElseThrow(() -> new RuntimeException("Submission not found"));
        if (!submission.getStudent().getUsername().equals(username)) {
            throw new RuntimeException("无权下载他人证书");
        }
        if (!submission.getExam().getId().equals(examId)) {
            throw new RuntimeException("答卷与考试不匹配");
        }

        return certificateRepository.findByExamIdAndSubmissionId(examId, submissionId)
                .orElseGet(() -> createCertificate(exam, submission));
    }

    private Certificate createCertificate(Exam exam, Submission submission) {
        User student = submission.getStudent();

        Integer totalScore = examQuestionRepository.sumScoreByExamId(exam.getId());
        int total = totalScore != null ? totalScore : 0;
        int passScore = exam.getCertPassScore() != null ? exam.getCertPassScore() : (int) (total * 0.6);
        int score = submission.getScore() != null ? submission.getScore() : 0;

        if (score < passScore) {
            throw new RuntimeException("成绩未达到分数线，无法生成证书");
        }

        Certificate cert = new Certificate();
        cert.setCertificateNo("EXAM-" + exam.getId() + "-" + submission.getId());
        cert.setExam(exam);
        cert.setSubmission(submission);
        cert.setStudent(student);
        cert.setStudentName(student.getFullName());
        cert.setStudentUsername(student.getUsername());
        cert.setExamTitle(exam.getCertTitle() != null && !exam.getCertTitle().isBlank() ? exam.getCertTitle() : exam.getTitle());
        cert.setScore(score);
        cert.setTotalScore(total);
        cert.setIssuer(exam.getCertIssuer());
        cert.setIssueDate(LocalDateTime.now());

        return certificateRepository.save(cert);
    }

    public byte[] generateCertificatePdf(Long examId, Long submissionId, String username) {
        Certificate cert = getOrCreateCertificate(examId, submissionId, username);
        cert.setDownloadCount(cert.getDownloadCount() + 1);
        certificateRepository.save(cert);

        return buildPdf(cert);
    }

    public byte[] generatePreviewPdf(Long examId) {
        Exam exam = examRepository.findById(examId).orElseThrow(() -> new RuntimeException("Exam not found"));
        if (!Boolean.TRUE.equals(exam.getEnableCert())) {
            throw new RuntimeException("该考试未启用合格证书");
        }

        Certificate preview = new Certificate();
        preview.setCertificateNo("EXAM-" + examId + "-PREVIEW");
        preview.setStudentName("张三");
        preview.setStudentUsername("STU001");
        preview.setExamTitle(exam.getCertTitle() != null && !exam.getCertTitle().isBlank() ? exam.getCertTitle() : exam.getTitle());
        Integer totalScore = examQuestionRepository.sumScoreByExamId(examId);
        int total = totalScore != null ? totalScore : 100;
        int passScore = exam.getCertPassScore() != null ? exam.getCertPassScore() : (int) (total * 0.6);
        preview.setScore(Math.max(passScore, (int) (total * 0.8)));
        preview.setTotalScore(total);
        preview.setIssuer(exam.getCertIssuer() != null ? exam.getCertIssuer() : "");
        preview.setIssueDate(LocalDateTime.now());

        return buildPdf(preview);
    }

    private byte[] buildPdf(Certificate cert) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            Document document = new Document(PageSize.A4.rotate(), 60, 60, 60, 60);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = createFont(32, Font.BOLD, new Color(26, 54, 110));
            Font subtitleFont = createFont(18, Font.BOLD, new Color(80, 80, 80));
            Font labelFont = createFont(14, Font.BOLD, new Color(50, 50, 50));
            Font valueFont = createFont(14, Font.NORMAL, new Color(30, 30, 30));
            Font certNoFont = createFont(11, Font.NORMAL, new Color(150, 150, 150));
            Font sealFont = createFont(16, Font.BOLD, new Color(180, 40, 40));
            Font certifyFont = createFont(15, Font.NORMAL, new Color(40, 40, 40));

            PdfPTable borderTable = new PdfPTable(1);
            borderTable.setWidthPercentage(100);
            PdfPCell borderCell = new PdfPCell();
            borderCell.setBorder(Rectangle.BOX);
            borderCell.setBorderWidth(4);
            borderCell.setBorderColor(new Color(26, 54, 110));
            borderCell.setPadding(50);

            PdfPTable innerTable = new PdfPTable(1);
            innerTable.setWidthPercentage(100);

            PdfPCell titleCell = new PdfPCell(new Phrase("合 格 证 书", titleFont));
            titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            titleCell.setBorder(Rectangle.NO_BORDER);
            titleCell.setPaddingBottom(16);
            innerTable.addCell(titleCell);

            PdfPCell examTitleCell = new PdfPCell(new Phrase(cert.getExamTitle(), subtitleFont));
            examTitleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            examTitleCell.setBorder(Rectangle.NO_BORDER);
            examTitleCell.setPaddingBottom(36);
            innerTable.addCell(examTitleCell);

            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(70);
            infoTable.setWidths(new float[]{1, 2});
            infoTable.setHorizontalAlignment(Element.ALIGN_CENTER);

            addInfoRow(infoTable, "姓\u3000\u3000名：", cert.getStudentName(), labelFont, valueFont);
            addInfoRow(infoTable, "学\u3000\u3000号：", cert.getStudentUsername(), labelFont, valueFont);
            addInfoRow(infoTable, "考试名称：", cert.getExamTitle(), labelFont, valueFont);
            addInfoRow(infoTable, "考试成绩：", cert.getScore() + " 分（满分 " + cert.getTotalScore() + " 分）", labelFont, valueFont);

            PdfPCell infoCell = new PdfPCell(infoTable);
            infoCell.setBorder(Rectangle.NO_BORDER);
            infoCell.setPaddingBottom(36);
            innerTable.addCell(infoCell);

            PdfPCell certifyCell = new PdfPCell(new Phrase("经考核，该同学已达到合格标准，特发此证。", certifyFont));
            certifyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            certifyCell.setBorder(Rectangle.NO_BORDER);
            certifyCell.setPaddingBottom(50);
            innerTable.addCell(certifyCell);

            PdfPTable bottomTable = new PdfPTable(2);
            bottomTable.setWidthPercentage(80);
            bottomTable.setHorizontalAlignment(Element.ALIGN_CENTER);

            PdfPCell issuerCell = new PdfPCell(new Phrase("发证单位：" + (cert.getIssuer() != null ? cert.getIssuer() : ""), valueFont));
            issuerCell.setBorder(Rectangle.NO_BORDER);
            issuerCell.setPaddingBottom(10);
            bottomTable.addCell(issuerCell);

            String dateStr = cert.getIssueDate() != null ? cert.getIssueDate().format(DateTimeFormatter.ofPattern("yyyy 年 MM 月 dd 日")) : "";
            PdfPCell dateCell = new PdfPCell(new Phrase("发证日期：" + dateStr, valueFont));
            dateCell.setBorder(Rectangle.NO_BORDER);
            dateCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            dateCell.setPaddingBottom(10);
            bottomTable.addCell(dateCell);

            PdfPCell sealCell = new PdfPCell(new Phrase(""));
            sealCell.setBorder(Rectangle.NO_BORDER);
            bottomTable.addCell(sealCell);

            PdfPCell sealRightCell = new PdfPCell(new Phrase("◆ 合 格 印 章 ◆", sealFont));
            sealRightCell.setBorder(Rectangle.NO_BORDER);
            sealRightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            sealRightCell.setPaddingTop(8);
            bottomTable.addCell(sealRightCell);

            PdfPCell certNoCell = new PdfPCell(new Phrase("证书编号：" + cert.getCertificateNo(), certNoFont));
            certNoCell.setBorder(Rectangle.NO_BORDER);
            certNoCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            certNoCell.setColspan(2);
            certNoCell.setPaddingTop(24);
            bottomTable.addCell(certNoCell);

            PdfPCell bottomCell = new PdfPCell(bottomTable);
            bottomCell.setBorder(Rectangle.NO_BORDER);
            innerTable.addCell(bottomCell);

            borderCell.addElement(innerTable);
            borderTable.addCell(borderCell);

            document.add(borderTable);
            document.close();
        } catch (Exception e) {
            throw new RuntimeException("PDF 生成失败", e);
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
