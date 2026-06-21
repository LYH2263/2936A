package com.exam.repository;

import com.exam.entity.QuestionFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface QuestionFeedbackRepository extends JpaRepository<QuestionFeedback, Long> {
    List<QuestionFeedback> findByStudentUsernameOrderByCreatedAtDesc(String username);

    List<QuestionFeedback> findByStatusOrderByCreatedAtAsc(String status);

    long countByStatus(String status);

    boolean existsByQuestionIdAndStudentIdAndCreatedAtAfter(Long questionId, Long studentId, LocalDateTime since);

    List<QuestionFeedback> findByQuestionIdAndStudentIdAndCreatedAtAfter(Long questionId, Long studentId, LocalDateTime since);

    @Query("SELECT fb FROM QuestionFeedback fb WHERE fb.status = 'PENDING' ORDER BY fb.createdAt ASC")
    List<QuestionFeedback> findPendingOrderByCreatedAt();

    @Query("SELECT fb.question.id, COUNT(fb) FROM QuestionFeedback fb WHERE fb.status = 'PENDING' GROUP BY fb.question.id ORDER BY COUNT(fb) DESC")
    List<Object[]> countPendingGroupedByQuestion();

    @Query("SELECT fb FROM QuestionFeedback fb WHERE fb.status = 'PENDING' ORDER BY " +
           "(SELECT COUNT(fb2) FROM QuestionFeedback fb2 WHERE fb2.question.id = fb.question.id AND fb2.status = 'PENDING') DESC, fb.createdAt ASC")
    List<QuestionFeedback> findPendingOrderByQuestionPendingCount();
}
