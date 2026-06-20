package com.exam.repository;

import com.exam.entity.StudyPlanTask;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudyPlanTaskRepository extends JpaRepository<StudyPlanTask, Long> {
    List<StudyPlanTask> findByPlanIdOrderBySortOrderAsc(Long planId);
    void deleteByPlanId(Long planId);
}
