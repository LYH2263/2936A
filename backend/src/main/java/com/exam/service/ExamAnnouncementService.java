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

        if (!"PUBLISHED".equals(exam.getState())) {
            throw new RuntimeException("Announcements can only be created for PUBLISHED exams");
        }

        long count = announcementRepository.countByExamId(examId);
        if (count >= MAX_ANNOUNCEMENTS) {
            throw new RuntimeException("Maximum " + MAX_ANNOUNCEMENTS + " announcements allowed per exam");
        }

        User creator = userRepository.findByUsername(creatorUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

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

        if (!announcement.getCreator().getId().equals(user.getId())
                && !"ADMIN".equals(user.getRole())) {
            throw new RuntimeException("No permission to edit this announcement");
        }

        if (dto.getTitle() != null) {
            announcement.setTitle(dto.getTitle());
        }
        if (dto.getContent() != null) {
            announcement.setContent(dto.getContent());
        }
        if (dto.getIsPinned() != null) {
            announcement.setIsPinned(dto.getIsPinned());
        }

        return announcementRepository.save(announcement);
    }

    @Transactional
    public ExamAnnouncement togglePin(Long announcementId, String username) {
        ExamAnnouncement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new RuntimeException("Announcement not found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!announcement.getCreator().getId().equals(user.getId())
                && !"ADMIN".equals(user.getRole())) {
            throw new RuntimeException("No permission to modify this announcement");
        }

        announcement.setIsPinned(!announcement.getIsPinned());
        return announcementRepository.save(announcement);
    }

    @Transactional
    public void deleteAnnouncement(Long announcementId, String username) {
        ExamAnnouncement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new RuntimeException("Announcement not found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!announcement.getCreator().getId().equals(user.getId())
                && !"ADMIN".equals(user.getRole())) {
            throw new RuntimeException("No permission to delete this announcement");
        }

        announcementRepository.delete(announcement);
    }

    public List<Map<String, Object>> getAnnouncementsForTeacher(Long examId) {
        List<ExamAnnouncement> announcements = announcementRepository
                .findByExamIdOrderByIsPinnedDescCreatedAtDesc(examId);

        return announcements.stream().map(this::toTeacherMap).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getAnnouncementsForStudent(Long examId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<ExamAnnouncement> announcements = announcementRepository
                .findByExamIdOrderByIsPinnedDescCreatedAtDesc(examId);

        Set<Long> readAnnouncementIds = readRepository
                .findByAnnouncementExamIdAndUser(examId, user)
                .stream()
                .map(r -> r.getAnnouncement().getId())
                .collect(Collectors.toSet());

        return announcements.stream().map(a -> {
            Map<String, Object> map = toStudentMap(a);
            map.put("isRead", readAnnouncementIds.contains(a.getId()));
            return map;
        }).collect(Collectors.toList());
    }

    public long getUnreadCount(Long examId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<ExamAnnouncement> announcements = announcementRepository
                .findByExamIdOrderByIsPinnedDescCreatedAtDesc(examId);

        Set<Long> readIds = readRepository
                .findByAnnouncementExamIdAndUser(examId, user)
                .stream()
                .map(r -> r.getAnnouncement().getId())
                .collect(Collectors.toSet());

        return announcements.stream()
                .filter(a -> !readIds.contains(a.getId()))
                .count();
    }

    @Transactional
    public void markAsRead(Long announcementId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ExamAnnouncement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new RuntimeException("Announcement not found"));

        if (readRepository.existsByAnnouncementIdAndUser(announcementId, user)) {
            return;
        }

        ExamAnnouncementRead read = new ExamAnnouncementRead();
        read.setAnnouncement(announcement);
        read.setUser(user);
        readRepository.save(read);
    }

    @Transactional
    public void markAllAsRead(Long examId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<ExamAnnouncement> announcements = announcementRepository
                .findByExamIdOrderByIsPinnedDescCreatedAtDesc(examId);

        Set<Long> readIds = readRepository
                .findByAnnouncementExamIdAndUser(examId, user)
                .stream()
                .map(r -> r.getAnnouncement().getId())
                .collect(Collectors.toSet());

        for (ExamAnnouncement a : announcements) {
            if (!readIds.contains(a.getId())) {
                ExamAnnouncementRead read = new ExamAnnouncementRead();
                read.setAnnouncement(a);
                read.setUser(user);
                readRepository.save(read);
            }
        }
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
