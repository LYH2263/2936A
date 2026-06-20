package com.exam.repository;

import com.exam.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByStudentUsername(String username);
    List<Submission> findByExamId(Long examId);
    List<Submission> findByExamIdAndStudentUsername(Long examId, String username);
    List<Submission> findTop5ByOrderByEndTimeDesc();
    List<Submission> findByExamIdAndState(Long examId, String state);

    @Query("SELECT COUNT(DISTINCT s.student.id) FROM Submission s")
    long countDistinctStudents();

    @Query("SELECT COUNT(s) FROM Submission s WHERE s.exam.id = :examId AND s.state = :state")
    long countByExamIdAndState(@Param("examId") Long examId, @Param("state") String state);
}
