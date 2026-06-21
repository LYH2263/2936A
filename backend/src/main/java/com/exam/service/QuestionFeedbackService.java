package com.exam.service;

import com.exam.entity.QuestionFeedback;
import com.exam.entity.Question;
import com.exam.entity.Submission;
import com.exam.entity.SubmissionAnswer;
import com.exam.entity.User;
import com.exam.repository.QuestionFeedbackRepository;
import com.exam.repository.QuestionRepository;
import com.exam.repository.SubmissionAnswerRepository;
import com.exam.repository.SubmissionRepository;
import com.exam.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class QuestionFeedbackService {

    private final QuestionFeedbackRepository feedbackRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final BadgeService badgeService;
    private final SubmissionRepository submissionRepository;
    private final SubmissionAnswerRepository submissionAnswerRepository;

    public QuestionFeedbackService(QuestionFeedbackRepository feedbackRepository,
                                   QuestionRepository questionRepository,
                                   UserRepository userRepository,
                                   NotificationService notificationService,
                                   BadgeService badgeService,
                                   SubmissionRepository submissionRepository,
                                   SubmissionAnswerRepository submissionAnswerRepository) {
        this.feedbackRepository = feedbackRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.badgeService = badgeService;
        this.submissionRepository = submissionRepository;
        this.submissionAnswerRepository = submissionAnswerRepository;
    }

    @Transactional
    public QuestionFeedback submitFeedback(Long questionId, Long submissionId, String type, String description, String username) {
        User student = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("题目不存在"));

        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("答卷不存在"));

        if (!submission.getStudent().getId().equals(student.getId())) {
            throw new RuntimeException("只能对自己的答卷提交纠错");
        }

        if (!Boolean.TRUE.equals(submission.getExam().getAllowViewAnalysis())) {
            throw new RuntimeException("该考试未开放解析，无法提交纠错");
        }

        SubmissionAnswer sa = submissionAnswerRepository.findBySubmissionIdAndQuestionId(submissionId, questionId)
                .orElseThrow(() -> new RuntimeException("您未作答过该题，无法提交纠错"));

        if (sa.getStudentAnswer() == null || sa.getStudentAnswer().trim().isEmpty()) {
            throw new RuntimeException("您未作答过该题，无法提交纠错");
        }

        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        List<QuestionFeedback> recentFeedbacks = feedbackRepository.findByQuestionIdAndStudentIdAndCreatedAtAfter(
                questionId, student.getId(), twentyFourHoursAgo);

        boolean hasPending = recentFeedbacks.stream().anyMatch(f -> "PENDING".equals(f.getStatus()));
        boolean hasConfirmed = recentFeedbacks.stream().anyMatch(f -> "CONFIRMED".equals(f.getStatus()));
        boolean hasRecentRejected = recentFeedbacks.stream().anyMatch(f -> "REJECTED".equals(f.getStatus()));

        if (hasPending) {
            throw new RuntimeException("该题纠错正在处理中，请等待处理结果");
        }
        if (hasConfirmed) {
            throw new RuntimeException("该题纠错已被确认，无需重复提交");
        }
        if (hasRecentRejected) {
            throw new RuntimeException("24小时内已提交过该题的纠错并被驳回，请稍后再试");
        }

        if (type == null || type.trim().isEmpty()) {
            throw new RuntimeException("请选择问题类型");
        }

        List<String> validTypes = List.of("ANSWER_ERROR", "QUESTION_UNCLEAR", "OPTION_DUPLICATE", "OTHER");
        if (!validTypes.contains(type)) {
            throw new RuntimeException("无效的问题类型");
        }

        if (description != null && description.length() > 500) {
            throw new RuntimeException("补充说明不能超过500字");
        }

        QuestionFeedback feedback = new QuestionFeedback();
        feedback.setQuestion(question);
        feedback.setStudent(student);
        feedback.setType(type);
        feedback.setDescription(description != null ? description.trim() : "");
        feedback.setStatus("PENDING");

        return feedbackRepository.save(feedback);
    }

    public List<QuestionFeedback> getMyFeedbacks(String username) {
        return feedbackRepository.findByStudentUsernameOrderByCreatedAtDesc(username);
    }

    public List<QuestionFeedback> getPendingFeedbacks() {
        return feedbackRepository.findPendingOrderByQuestionPendingCount();
    }

    public long getPendingCount() {
        return feedbackRepository.countByStatus("PENDING");
    }

    @Transactional
    public QuestionFeedback processFeedback(Long feedbackId, String action, String handlerComment, String username) {
        QuestionFeedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("纠错工单不存在"));

        if (!"PENDING".equals(feedback.getStatus())) {
            throw new RuntimeException("该工单已处理");
        }

        User handler = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("处理人不存在"));

        if (!"TEACHER".equals(handler.getRole()) && !"ADMIN".equals(handler.getRole())) {
            throw new RuntimeException("只有教师才能处理纠错工单");
        }

        feedback.setHandler(handler);
        feedback.setHandlerComment(handlerComment);
        feedback.setHandledAt(LocalDateTime.now());

        if ("CONFIRM".equals(action)) {
            feedback.setStatus("CONFIRMED");
            badgeService.checkCorrectionExpert(feedback.getStudent());
        } else if ("REJECT".equals(action)) {
            feedback.setStatus("REJECTED");
        } else {
            throw new RuntimeException("无效的操作类型");
        }

        QuestionFeedback saved = feedbackRepository.save(feedback);

        String actionText = "CONFIRM".equals(action) ? "已确认" : "已驳回";
        String typeText = getTypeDisplayText(feedback.getType());
        notificationService.createNotification(
                feedback.getStudent(),
                "纠错处理结果通知",
                "您提交的题目纠错（" + typeText + "）" + actionText +
                        (handlerComment != null ? "，处理意见：" + handlerComment : ""),
                "FEEDBACK_RESULT",
                feedback.getId()
        );

        return saved;
    }

    public QuestionFeedback getFeedbackById(Long feedbackId) {
        return feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("纠错工单不存在"));
    }

    private String getTypeDisplayText(String type) {
        return switch (type) {
            case "ANSWER_ERROR" -> "答案错误";
            case "QUESTION_UNCLEAR" -> "题干不清";
            case "OPTION_DUPLICATE" -> "选项重复";
            case "OTHER" -> "其他";
            default -> type;
        };
    }
}
