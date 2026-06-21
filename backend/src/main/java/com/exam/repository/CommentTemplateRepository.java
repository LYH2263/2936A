package com.exam.repository;

import com.exam.entity.CommentTemplate;
import com.exam.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CommentTemplateRepository extends JpaRepository<CommentTemplate, Long> {
    List<CommentTemplate> findByTeacherOrderByCreatedAtDesc(User teacher);

    List<CommentTemplate> findByIsPublicTrueOrderByCreatedAtDesc();

    @Query("SELECT ct FROM CommentTemplate ct WHERE ct.teacher = :teacher OR ct.isPublic = true ORDER BY ct.createdAt DESC")
    List<CommentTemplate> findVisibleToTeacher(@Param("teacher") User teacher);

    List<CommentTemplate> findByTeacherAndSubjectOrderByCreatedAtDesc(User teacher, String subject);

    @Query("SELECT ct FROM CommentTemplate ct WHERE (ct.teacher = :teacher OR ct.isPublic = true) AND (ct.subject = :subject OR ct.subject IS NULL OR ct.subject = '') ORDER BY CASE WHEN ct.subject = :subject THEN 0 ELSE 1 END, ct.createdAt DESC")
    List<CommentTemplate> findVisibleToTeacherBySubject(@Param("teacher") User teacher, @Param("subject") String subject);
}
