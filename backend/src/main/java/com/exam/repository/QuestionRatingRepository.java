package com.exam.repository;

import com.exam.entity.QuestionRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuestionRatingRepository extends JpaRepository<QuestionRating, Long> {

    Optional<QuestionRating> findByQuestionIdAndStudentId(Long questionId, Long studentId);

    Optional<QuestionRating> findByQuestionIdAndStudentUsername(Long questionId, String username);

    List<QuestionRating> findByStudentUsernameOrderByUpdatedAtDesc(String username);

    List<QuestionRating> findByQuestionId(Long questionId);

    @Query("SELECT AVG(r.rating) FROM QuestionRating r WHERE r.question.id = :questionId")
    Double findAverageRatingByQuestionId(@Param("questionId") Long questionId);

    @Query("SELECT COUNT(r) FROM QuestionRating r WHERE r.question.id = :questionId")
    Long countByQuestionId(@Param("questionId") Long questionId);

    @Query("SELECT r.question.id, AVG(r.rating), COUNT(r) FROM QuestionRating r WHERE r.question.id IN :questionIds GROUP BY r.question.id")
    List<Object[]> findAverageAndCountByQuestionIds(@Param("questionIds") List<Long> questionIds);
}
