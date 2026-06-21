package com.exam.service;

import com.exam.dto.ExamAnnouncementDTO;
import com.exam.entity.Exam;
import com.exam.entity.ExamAnnouncement;
import com.exam.entity.ExamAnnouncementRead;
import com.exam.entity.User;
import com.exam.repository.ExamAnnouncementReadRepository;
import com.exam.repository.ExamAnnouncementRepository;
import com.exam.repository.ExamRepository;
import com.exam.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExamAnnouncementService {
    private static final int MAX_ANNOUNCEMENTS = 50;

    private final ExamAnnouncementRepository announcementRepository;
    private final ExamAnnouncementReadRepository readRepository;
    private final ExamRepository examRepository;
    private final UserRepository userRepository;

    public ExamAnnouncementService(ExamAnnouncementRepository announcementRepository,
                                   ExamAnnouncementReadRepository readRepository,
                                   ExamRepository examRepository,
                                   UserRepository userRepository) {
        this.announcementRepository = announcementRepository;
        this.readRepository = readRepository;
        this.examRepository = examRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ExamAnnouncement createAnnouncement(Long examId, ExamAnnouncementDTO dto, String creatorUsername) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found"));

        User creator = userRepository.findByUsername(creatorUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        checkExamManagePermission(exam, creator);

        if (!"PUBLISHED".equals(exam.getState())) {
            throw new RuntimeException("Announcements can only be created for PUBLISHED exams");
        }

        long count = announcementRepository.countByExamId(examId);
        if (count >= MAX_ANNOUNCEMENTS) {
            throw new RuntimeException("Maximum " + MAX_ANNOUNCEMENTS + " announcements allowed per exam");
        }

        ExamAnnouncement announcement = new ExamAnnouncement();
        announcement.setExam(exam);
        announcement.setTitle(dto.getTitle());
        announcement.setContent(dto.getContent());
        announcement.setIsPinned(Boolean.TRUE.equals(dto.getIsPinned()));
        announcement.setCreator(creator);

        return announcementRepository.save(announcement);
    }

    @Transactional
    public ExamAnnouncement updateAnnouncement(Long announcementId, ExamAnnouncementDTO dto, String username) {
        ExamAnnouncement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new RuntimeException("Announcement not found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        checkExamManagePermission(announcement.getExam(), user);

        boolean hasContentChange = false;
        if (dto.getTitle() != null && !dto.getTitle().equals(announcement.getTitle())) {
            announcement.setTitle(dto.getTitle());
            hasContentChange = true;
        }
        if (dto.getContent() != null && !dto.getContent().equals(announcement.getContent())) {
            announcement.setContent(dto.getContent());
            hasContentChange = true;
        }
        if (dto.getIsPinned() != null) {
            announcement.setIsPinned(dto.getIsPinned());
        }

        if (hasContentChange) {
            announcement.setUpdatedAt(LocalDateTime.now());
        }

        return announcementRepository.save(announcement);
    }

    @Transactional
    public ExamAnnouncement togglePin(Long announcementId, String username) {
        ExamAnnouncement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new RuntimeException("Announcement not found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        checkExamManagePermission(announcement.getExam(), user);

        announcement.setIsPinned(!announcement.getIsPinned());
        return announcementRepository.save(announcement);
    }

    @Transactional
    public void deleteAnnouncement(Long announcementId, String username) {
        ExamAnnouncement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new RuntimeException("Announcement not found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        checkExamManagePermission(announcement.getExam(), user);

        announcementRepository.delete(announcement);
    }

    public List<Map<String, Object>> getAnnouncementsForTeacher(Long examId, String username) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        checkExamManagePermission(exam, user);

        List<ExamAnnouncement> announcements = announcementRepository
                .findByExamIdOrderByIsPinnedDescCreatedAtDesc(examId);

        return announcements.stream().map(this::toTeacherMap).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getAnnouncementsForStudent(Long examId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found"));

        checkStudentVisibility(exam, user);

        List<ExamAnnouncement> announcements = announcementRepository
                .findByExamIdOrderByIsPinnedDescCreatedAtDesc(examId);

        Map<Long, LocalDateTime> readMap = readRepository
                .findByAnnouncementExamIdAndUser(examId, user)
                .stream()
                .collect(Collectors.toMap(
                        r -> r.getAnnouncement().getId(),
                        ExamAnnouncementRead::getReadAt
                ));

        return announcements.stream().map(a -> {
            Map<String, Object> map = toStudentMap(a);
            LocalDateTime readAt = readMap.get(a.getId());
            boolean isRead = readAt != null
                    && a.getUpdatedAt() != null
                    && !readAt.isBefore(a.getUpdatedAt());
            map.put("isRead", isRead);
            return map;
        }).collect(Collectors.toList());
    }

    public long getUnreadCount(Long examId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found"));

        checkStudentVisibility(exam, user);

        List<ExamAnnouncement> announcements = announcementRepository
                .findByExamIdOrderByIsPinnedDescCreatedAtDesc(examId);

        Map<Long, LocalDateTime> readMap = readRepository
                .findByAnnouncementExamIdAndUser(examId, user)
                .stream()
                .collect(Collectors.toMap(
                        r -> r.getAnnouncement().getId(),
                        ExamAnnouncementRead::getReadAt
                ));

        return announcements.stream()
                .filter(a -> {
                    LocalDateTime readAt = readMap.get(a.getId());
                    return readAt == null
                            || a.getUpdatedAt() == null
                            || readAt.isBefore(a.getUpdatedAt());
                })
                .count();
    }

    @Transactional
    public void markAsRead(Long announcementId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ExamAnnouncement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new RuntimeException("Announcement not found"));

        checkStudentVisibility(announcement.getExam(), user);

        Optional<ExamAnnouncementRead> existingRead = readRepository
                .findByAnnouncementExamIdAndUser(announcement.getExam().getId(), user)
                .stream()
                .filter(r -> r.getAnnouncement().getId().equals(announcementId))
                .findFirst();

        if (existingRead.isPresent()) {
            ExamAnnouncementRead read = existingRead.get();
            read.setReadAt(LocalDateTime.now());
            readRepository.save(read);
        } else {
            ExamAnnouncementRead read = new ExamAnnouncementRead();
            read.setAnnouncement(announcement);
            read.setUser(user);
            readRepository.save(read);
        }
    }

    @Transactional
    public void markAllAsRead(Long examId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found"));

        checkStudentVisibility(exam, user);

        List<ExamAnnouncement> announcements = announcementRepository
                .findByExamIdOrderByIsPinnedDescCreatedAtDesc(examId);

        Map<Long, ExamAnnouncementRead> readMap = readRepository
                .findByAnnouncementExamIdAndUser(examId, user)
                .stream()
                .collect(Collectors.toMap(
                        r -> r.getAnnouncement().getId(),
                        r -> r
                ));

        LocalDateTime now = LocalDateTime.now();
        for (ExamAnnouncement a : announcements) {
            ExamAnnouncementRead existing = readMap.get(a.getId());
            if (existing != null) {
                existing.setReadAt(now);
                readRepository.save(existing);
            } else {
                ExamAnnouncementRead read = new ExamAnnouncementRead();
                read.setAnnouncement(a);
                read.setUser(user);
                readRepository.save(read);
            }
        }
    }

    private void checkExamManagePermission(Exam exam, User user) {
        if ("ADMIN".equals(user.getRole())) {
            return;
        }
        if (exam.getCreator() != null && exam.getCreator().getId().equals(user.getId())) {
            return;
        }
        throw new RuntimeException("No permission to manage announcements for this exam");
    }

    private void checkStudentVisibility(Exam exam, User user) {
        if ("ADMIN".equals(user.getRole()) || "TEACHER".equals(user.getRole())) {
            return;
        }
        if (!"PUBLISHED".equals(exam.getState())) {
            throw new RuntimeException("Exam is not visible");
        }
        if ("ALL".equals(exam.getTargetAudience()) || exam.getTargetAudience() == null) {
            return;
        }
        if ("CUSTOM".equals(exam.getTargetAudience()) && exam.getTargetIds() != null) {
            String[] ids = exam.getTargetIds().split(",");
            for (String id : ids) {
                String trimmed = id.trim();
                if (trimmed.equals(user.getUsername())) {
                    return;
                }
                try {
                    Long numericId = Long.parseLong(trimmed);
                    if (numericId.equals(user.getId())) {
                        return;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        throw new RuntimeException("Exam is not visible to you");
    }

    private Map<String, Object> toBaseMap(ExamAnnouncement a) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", a.getId());
        map.put("title", a.getTitle());
        map.put("content", a.getContent());
        map.put("isPinned", a.getIsPinned());
        map.put("createdAt", a.getCreatedAt());
        map.put("updatedAt", a.getUpdatedAt());
        return map;
    }

    private Map<String, Object> toTeacherMap(ExamAnnouncement a) {
        Map<String, Object> map = toBaseMap(a);
        Map<String, Object> creator = new LinkedHashMap<>();
        creator.put("id", a.getCreator().getId());
        creator.put("username", a.getCreator().getUsername());
        creator.put("fullName", a.getCreator().getFullName());
        map.put("creator", creator);
        return map;
    }

    private Map<String, Object> toStudentMap(ExamAnnouncement a) {
        return toBaseMap(a);
    }
}
