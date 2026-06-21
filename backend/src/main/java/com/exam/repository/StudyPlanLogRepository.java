package com.exam.repository;

import com.exam.entity.StudyPlanLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StudyPlanLogRepository extends JpaRepository<StudyPlanLog, Long> {
    Optional<StudyPlanLog> findByPlanIdAndLogDate(Long planId, java.time.LocalDate logDate);
    void deleteByPlanId(Long planId);
}
