package com.exam.repository;

import com.exam.entity.ExamAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExamAnnouncementRepository extends JpaRepository<ExamAnnouncement, Long> {
    List<ExamAnnouncement> findByExamIdOrderByIsPinnedDescCreatedAtDesc(Long examId);
    List<ExamAnnouncement> findByExamIdOrderByCreatedAtDesc(Long examId);
    long countByExamId(Long examId);
}
