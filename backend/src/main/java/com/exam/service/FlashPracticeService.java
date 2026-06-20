package com.exam.service;

import com.exam.entity.*;
import com.exam.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FlashPracticeService {

    private final FlashPracticeSessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;

    public FlashPracticeService(FlashPracticeSessionRepository sessionRepository,
                                 QuestionRepository questionRepository,
                                 UserRepository userRepository) {
        this.sessionRepository = sessionRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
    }

    public List<String> getAllSubjects() {
        return questionRepository.findAllSubjects();
    }

    public List<String> getKnowledgePointsBySubject(String subject) {
        if (subject == null || subject.isEmpty()) {
            return questionRepository.findAllKnowledgePoints();
        }
        return questionRepository.findKnowledgePointsBySubject(subject);
    }

    public Map<String, Object> getTodayStats(String username) {
        LocalDate today = LocalDate.now();
        Integer totalQ = sessionRepository.countQuestionsByUsernameAndDate(username, today);
        Integer correctQ = sessionRepository.countCorrectByUsernameAndDate(username, today);
        totalQ = totalQ != null ? totalQ : 0;
        correctQ = correctQ != null ? correctQ : 0;
        double accuracy = totalQ > 0 ? (double) correctQ / totalQ * 100 : 0.0;

        Map<String, Object> result = new HashMap<>();
        result.put("totalQuestions", totalQ);
        result.put("correctCount", correctQ);
        result.put("accuracy", Math.round(accuracy * 10.0) / 10.0);
        return result;
    }

    @Transactional
    public FlashPracticeSession startSession(String username, String subject, Integer difficulty, String knowledgePoint) {
        User student = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        FlashPracticeSession session = new FlashPracticeSession();
        session.setStudent(student);
        session.setSubject(subject);
        session.setDifficulty(difficulty);
        session.setKnowledgePoint(knowledgePoint != null && knowledgePoint.isEmpty() ? null : knowledgePoint);
        session.setPracticeDate(LocalDate.now());
        session.setStartTime(LocalDateTime.now());
        session.setTotalQuestions(0);
        session.setCorrectCount(0);
        session.setCurrentStreak(0);
        session.setMaxStreak(0);
        session.setAnsweredQuestionIds("");

        return sessionRepository.save(session);
    }

    public Question getNextQuestion(Long sessionId) {
        FlashPracticeSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        Set<Long> answeredIds = parseAnsweredIds(session.getAnsweredQuestionIds());

        List<Question> candidates = questionRepository.findRandomCandidates(
                session.getSubject(),
                session.getDifficulty(),
                session.getKnowledgePoint(),
                answeredIds
        );

        if (candidates.isEmpty()) {
            answeredIds.clear();
            candidates = questionRepository.findRandomCandidates(
                    session.getSubject(),
                    session.getDifficulty(),
                    session.getKnowledgePoint(),
                    answeredIds
            );
        }

        if (candidates.isEmpty()) {
            throw new RuntimeException("暂无符合条件的题目，请调整筛选条件");
        }

        int randomIdx = new Random().nextInt(candidates.size());
        Question q = candidates.get(randomIdx);
        Question result = new Question();
        result.setId(q.getId());
        result.setContent(q.getContent());
        result.setType(q.getType());
        result.setOptions(q.getOptions());
        result.setSubject(q.getSubject());
        result.setKnowledgePoint(q.getKnowledgePoint());
        result.setDifficulty(q.getDifficulty());
        result.setDefaultScore(q.getDefaultScore());
        return result;
    }

    @Transactional
    public Map<String, Object> submitAnswer(Long sessionId, Long questionId, String studentAnswer) {
        FlashPracticeSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        boolean isCorrect = autoGrade(question, studentAnswer);

        session.setTotalQuestions(session.getTotalQuestions() + 1);
        if (isCorrect) {
            session.setCorrectCount(session.getCorrectCount() + 1);
            session.setCurrentStreak(session.getCurrentStreak() + 1);
            if (session.getCurrentStreak() > session.getMaxStreak()) {
                session.setMaxStreak(session.getCurrentStreak());
            }
        } else {
            session.setCurrentStreak(0);
        }

        Set<Long> answeredIds = parseAnsweredIds(session.getAnsweredQuestionIds());
        answeredIds.add(questionId);
        session.setAnsweredQuestionIds(answeredIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",")));
        session.setEndTime(LocalDateTime.now());
        sessionRepository.save(session);

        Map<String, Object> result = new HashMap<>();
        result.put("correct", isCorrect);
        result.put("correctAnswer", question.getAnswer());
        result.put("analysis", question.getAnalysis());
        result.put("totalQuestions", session.getTotalQuestions());
        result.put("correctCount", session.getCorrectCount());
        result.put("currentStreak", session.getCurrentStreak());
        result.put("maxStreak", session.getMaxStreak());
        double accuracy = session.getTotalQuestions() > 0
                ? (double) session.getCorrectCount() / session.getTotalQuestions() * 100
                : 0.0;
        result.put("accuracy", Math.round(accuracy * 10.0) / 10.0);
        return result;
    }

    @Transactional
    public void endSession(Long sessionId) {
        FlashPracticeSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        session.setEndTime(LocalDateTime.now());
        sessionRepository.save(session);
    }

    public FlashPracticeSession getSession(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
    }

    private boolean autoGrade(Question question, String studentAnswer) {
        if (studentAnswer == null || studentAnswer.trim().isEmpty()) {
            return false;
        }
        String answer = question.getAnswer();
        if (answer == null) return false;

        String type = question.getType();
        if ("SINGLE".equals(type) || "JUDGE".equals(type)) {
            return studentAnswer.trim().equals(answer.trim());
        } else if ("MULTI".equals(type)) {
            List<String> userOpts = Arrays.stream(studentAnswer.split(",|\\s+"))
                    .map(String::trim).filter(s -> !s.isEmpty()).sorted().collect(Collectors.toList());
            List<String> correctOpts = Arrays.stream(answer.split(",|\\s+"))
                    .map(String::trim).filter(s -> !s.isEmpty()).sorted().collect(Collectors.toList());
            return userOpts.equals(correctOpts);
        } else if ("SHORT".equals(type)) {
            String[] keywords = answer.split(";|\\s+");
            int matches = 0;
            int validKeywords = 0;
            for (String keyword : keywords) {
                if (!keyword.trim().isEmpty()) {
                    validKeywords++;
                    if (studentAnswer.contains(keyword.trim())) {
                        matches++;
                    }
                }
            }
            return validKeywords > 0 && matches == validKeywords;
        }
        return false;
    }

    private Set<Long> parseAnsweredIds(String str) {
        Set<Long> ids = new HashSet<>();
        if (str == null || str.trim().isEmpty()) return ids;
        for (String s : str.split(",")) {
            try {
                ids.add(Long.parseLong(s.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return ids;
    }
}
