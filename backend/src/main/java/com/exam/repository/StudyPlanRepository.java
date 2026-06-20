package com.exam.repository;

import com.exam.entity.StudyPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface StudyPlanRepository extends JpaRepository<StudyPlan, Long> {
    Optional<StudyPlan> findByUserIdAndExamId(Long userId, Long examId);
    List<StudyPlan> findByUserIdAndStatus(Long userId, String status);
    List<StudyPlan> findByUserId(Long userId);
}
