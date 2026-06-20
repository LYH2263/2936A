package com.exam.repository;

import com.exam.entity.ExamAnnouncementRead;
import com.exam.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Set;

public interface ExamAnnouncementReadRepository extends JpaRepository<ExamAnnouncementRead, Long> {
    List<ExamAnnouncementRead> findByAnnouncementExamIdAndUser(Long examId, User user);
    boolean existsByAnnouncementIdAndUser(Long announcementId, User user);
    Set<ExamAnnouncementRead> findByAnnouncementExamIdAndUser_Id(Long examId, Long userId);
}
