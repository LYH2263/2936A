package com.exam.repository;

import com.exam.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Set;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findBySubject(String subject);
    List<Question> findBySubjectAndType(String subject, String type);
    List<Question> findByDifficulty(Integer difficulty);
    List<Question> findBySubjectAndDifficulty(String subject, Integer difficulty);
    List<Question> findByKnowledgePoint(String knowledgePoint);
    List<Question> findBySubjectAndKnowledgePoint(String subject, String knowledgePoint);
    List<Question> findByDifficultyAndKnowledgePoint(Integer difficulty, String knowledgePoint);
    List<Question> findBySubjectAndDifficultyAndKnowledgePoint(String subject, Integer difficulty, String knowledgePoint);

    @Query("SELECT DISTINCT q.subject FROM Question q WHERE q.subject IS NOT NULL AND q.subject <> ''")
    List<String> findAllSubjects();

    @Query("SELECT DISTINCT q.knowledgePoint FROM Question q WHERE q.knowledgePoint IS NOT NULL AND q.knowledgePoint <> ''")
    List<String> findAllKnowledgePoints();

    @Query("SELECT DISTINCT q.knowledgePoint FROM Question q WHERE q.subject = :subject AND q.knowledgePoint IS NOT NULL AND q.knowledgePoint <> ''")
    List<String> findKnowledgePointsBySubject(@Param("subject") String subject);

    @Query("SELECT q FROM Question q WHERE q.subject = :subject AND (:difficulty IS NULL OR q.difficulty = :difficulty) AND (:knowledgePoint IS NULL OR q.knowledgePoint = :knowledgePoint) AND q.id NOT IN :excludeIds")
    List<Question> findRandomCandidates(@Param("subject") String subject,
                                         @Param("difficulty") Integer difficulty,
                                         @Param("knowledgePoint") String knowledgePoint,
                                         @Param("excludeIds") Set<Long> excludeIds);
}
