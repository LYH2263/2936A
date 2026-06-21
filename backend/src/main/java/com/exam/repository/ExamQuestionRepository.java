package com.exam.repository;

import com.exam.entity.ExamQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, Long> {
    List<ExamQuestion> findByExamIdOrderBySequenceAsc(Long examId);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(eq.score) FROM ExamQuestion eq WHERE eq.exam.id = :examId")
    Integer sumScoreByExamId(@org.springframework.data.repository.query.Param("examId") Long examId);

    List<ExamQuestion> findByQuestionId(Long questionId);

    List<ExamQuestion> findByExamIdAndQuestionId(Long examId, Long questionId);

    int countByExamId(Long examId);
}
