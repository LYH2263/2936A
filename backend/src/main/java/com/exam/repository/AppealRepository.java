package com.exam.repository;

import com.exam.entity.Appeal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AppealRepository extends JpaRepository<Appeal, Long> {
    List<Appeal> findByStudentUsernameOrderByCreatedAtDesc(String username);
    List<Appeal> findByStatusOrderByCreatedAtAsc(String status);
    long countByStatus(String status);
    boolean existsByAnswerIdAndStatus(Long answerId, String status);
}
