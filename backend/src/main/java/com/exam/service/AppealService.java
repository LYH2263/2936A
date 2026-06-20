package com.exam.service;

import com.exam.entity.*;
import com.exam.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AppealService {

    private final AppealRepository appealRepository;
    private final SubmissionRepository submissionRepository;
    private final SubmissionAnswerRepository submissionAnswerRepository;
    private final UserRepository userRepository;
    private final BadgeService badgeService;

    public AppealService(AppealRepository appealRepository,
                         SubmissionRepository submissionRepository,
                         SubmissionAnswerRepository submissionAnswerRepository,
                         UserRepository userRepository,
                         BadgeService badgeService) {
        this.appealRepository = appealRepository;
        this.submissionRepository = submissionRepository;
        this.submissionAnswerRepository = submissionAnswerRepository;
        this.userRepository = userRepository;
        this.badgeService = badgeService;
    }

    @Transactional
    public Appeal submitAppeal(Long submissionId, Long answerId, String reason, String username) {
        User student = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("提交记录不存在"));

        if (!submission.getStudent().getId().equals(student.getId())) {
            throw new RuntimeException("无权对该提交发起申诉");
        }

        if (!"SUBMITTED".equals(submission.getState())) {
            throw new RuntimeException("只能对已提交的考试发起申诉");
        }

        SubmissionAnswer answer = submissionAnswerRepository.findById(answerId)
                .orElseThrow(() -> new RuntimeException("答案记录不存在"));

        if (!answer.getSubmission().getId().equals(submissionId)) {
            throw new RuntimeException("答案不属于该提交记录");
        }

        if (appealRepository.existsByAnswerIdAndStatus(answerId, "PENDING")) {
            throw new RuntimeException("该题目已有待处理的申诉");
        }

        if (reason == null || reason.trim().isEmpty()) {
            throw new RuntimeException("申诉理由不能为空");
        }

        if (reason.length() > 500) {
            throw new RuntimeException("申诉理由不能超过500字");
        }

        Appeal appeal = new Appeal();
        appeal.setSubmission(submission);
        appeal.setAnswer(answer);
        appeal.setStudent(student);
        appeal.setReason(reason.trim());
        appeal.setStatus("PENDING");

        return appealRepository.save(appeal);
    }

    public List<Appeal> getMyAppeals(String username) {
        return appealRepository.findByStudentUsernameOrderByCreatedAtDesc(username);
    }

    public List<Appeal> getPendingAppeals() {
        return appealRepository.findByStatusOrderByCreatedAtAsc("PENDING");
    }

    public long getPendingCount() {
        return appealRepository.countByStatus("PENDING");
    }

    @Transactional
    public Appeal processAppeal(Long appealId, String action, String handlerComment, Integer newScore, String username) {
        Appeal appeal = appealRepository.findById(appealId)
                .orElseThrow(() -> new RuntimeException("申诉不存在"));

        if (!"PENDING".equals(appeal.getStatus())) {
            throw new RuntimeException("该申诉已处理");
        }

        User handler = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("处理人不存在"));

        if (!"TEACHER".equals(handler.getRole()) && !"ADMIN".equals(handler.getRole())) {
            throw new RuntimeException("只有教师才能处理申诉");
        }

        appeal.setHandler(handler);
        appeal.setHandlerComment(handlerComment);
        appeal.setHandledAt(LocalDateTime.now());

        if ("APPROVE".equals(action)) {
            if (newScore == null) {
                throw new RuntimeException("改分时必须提供新分数");
            }
            appeal.setStatus("APPROVED");
            appeal.setNewScore(newScore);

            SubmissionAnswer answer = appeal.getAnswer();
            answer.setScore(newScore);
            submissionAnswerRepository.save(answer);

            Submission submission = appeal.getSubmission();
            int totalScore = 0;
            for (SubmissionAnswer sa : submission.getAnswers()) {
                totalScore += (sa.getScore() != null ? sa.getScore() : 0);
            }
            submission.setScore(totalScore);
            submissionRepository.save(submission);

            badgeService.checkAppealWin(appeal.getStudent());

        } else if ("REJECT".equals(action)) {
            appeal.setStatus("REJECTED");
        } else {
            throw new RuntimeException("无效的操作类型");
        }

        return appealRepository.save(appeal);
    }

    public Appeal getAppealById(Long appealId) {
        return appealRepository.findById(appealId)
                .orElseThrow(() -> new RuntimeException("申诉不存在"));
    }
}
