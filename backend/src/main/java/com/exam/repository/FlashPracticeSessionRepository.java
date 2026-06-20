package com.exam.repository;

import com.exam.entity.FlashPracticeSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FlashPracticeSessionRepository extends JpaRepository<FlashPracticeSession, Long> {

    @Query("SELECT s FROM FlashPracticeSession s WHERE s.student.username = :username AND s.practiceDate = :date ORDER BY s.startTime DESC")
    List<FlashPracticeSession> findByStudentUsernameAndDate(@Param("username") String username, @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(s.totalQuestions), 0) FROM FlashPracticeSession s WHERE s.student.username = :username AND s.practiceDate = :date")
    Integer countQuestionsByUsernameAndDate(@Param("username") String username, @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(s.correctCount), 0) FROM FlashPracticeSession s WHERE s.student.username = :username AND s.practiceDate = :date")
    Integer countCorrectByUsernameAndDate(@Param("username") String username, @Param("date") LocalDate date);

    List<FlashPracticeSession> findByStudentUsernameOrderByStartTimeDesc(String username);
}
