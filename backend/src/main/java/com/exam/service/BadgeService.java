package com.exam.service;

import com.exam.entity.Badge;
import com.exam.entity.User;
import com.exam.entity.UserBadge;
import com.exam.entity.Submission;
import com.exam.repository.UserBadgeRepository;
import com.exam.repository.SubmissionRepository;
import com.exam.repository.ExamQuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BadgeService {

    private static final Logger log = LoggerFactory.getLogger(BadgeService.class);

    private final UserBadgeRepository userBadgeRepository;
    private final SubmissionRepository submissionRepository;
    private final ExamQuestionRepository examQuestionRepository;

    public BadgeService(UserBadgeRepository userBadgeRepository,
                        SubmissionRepository submissionRepository,
                        ExamQuestionRepository examQuestionRepository) {
        this.userBadgeRepository = userBadgeRepository;
        this.submissionRepository = submissionRepository;
        this.examQuestionRepository = examQuestionRepository;
    }

    @Transactional
    public void awardBadge(User user, Badge badge) {
        if (userBadgeRepository.existsByUserIdAndBadgeCode(user.getId(), badge.name())) {
            return;
        }
        UserBadge ub = new UserBadge();
        ub.setUser(user);
        ub.setBadgeCode(badge.name());
        userBadgeRepository.save(ub);
        log.info("Awarded badge [{}] to user [{}]", badge.name(), user.getUsername());
    }

    public void checkSubmissionBadges(User student, Submission submission) {
        awardBadge(student, Badge.FIRST_SUBMIT);

        checkPerfectScore(student, submission);

        checkPassStreak(student);

        checkWeeklyActive(student);
    }

    public void checkAfterGrading(User student) {
        checkPassStreak(student);
    }

    public void checkAppealWin(User student) {
        awardBadge(student, Badge.APPEAL_WIN);
    }

    public void checkCorrectionExpert(User student) {
        awardBadge(student, Badge.CORRECTION_EXPERT);
    }

    private void checkPerfectScore(User student, Submission submission) {
        if (submission.getScore() == null) return;
        Integer totalScore = examQuestionRepository.sumScoreByExamId(submission.getExam().getId());
        if (totalScore != null && totalScore > 0 && submission.getScore().equals(totalScore)) {
            awardBadge(student, Badge.PERFECT_SCORE);
        }
    }

    private void checkPassStreak(User student) {
        List<Submission> subs = submissionRepository.findByStudentUsername(student.getUsername());
        List<Submission> submitted = subs.stream()
                .filter(s -> "SUBMITTED".equals(s.getState()) && s.getScore() != null)
                .sorted(Comparator.comparing(Submission::getEndTime))
                .collect(Collectors.toList());

        int streak = 0;
        for (Submission s : submitted) {
            Integer totalScore = examQuestionRepository.sumScoreByExamId(s.getExam().getId());
            int total = totalScore != null ? totalScore : 0;
            if (total > 0 && (double) s.getScore() / total >= 0.6) {
                streak++;
            } else {
                streak = 0;
            }
        }

        if (streak >= 3) {
            awardBadge(student, Badge.PASS_STREAK_3);
        }
    }

    private void checkWeeklyActive(User student) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekStart = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toLocalDate().atStartOfDay();

        List<Submission> subs = submissionRepository.findByStudentUsername(student.getUsername());
        long countThisWeek = subs.stream()
                .filter(s -> "SUBMITTED".equals(s.getState()))
                .filter(s -> s.getEndTime() != null && !s.getEndTime().isBefore(weekStart))
                .count();

        if (countThisWeek >= 3) {
            awardBadge(student, Badge.WEEKLY_ACTIVE);
        }
    }

    public List<Map<String, Object>> getMyBadges(Long userId) {
        List<UserBadge> earned = userBadgeRepository.findByUserIdOrderByEarnedAtDesc(userId);
        Set<String> earnedCodes = earned.stream()
                .map(UserBadge::getBadgeCode)
                .collect(Collectors.toSet());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Badge badge : Badge.values()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", badge.name());
            item.put("label", badge.getLabel());
            item.put("icon", badge.getIcon());
            item.put("description", badge.getDescription());
            item.put("earned", earnedCodes.contains(badge.name()));
            earned.stream()
                    .filter(ub -> ub.getBadgeCode().equals(badge.name()))
                    .findFirst()
                    .ifPresent(ub -> item.put("earnedAt", ub.getEarnedAt()));
            result.add(item);
        }
        return result;
    }
}
