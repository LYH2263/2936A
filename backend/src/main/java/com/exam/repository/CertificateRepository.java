package com.exam.repository;

import com.exam.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    Optional<Certificate> findByExamIdAndSubmissionId(Long examId, Long submissionId);
    Optional<Certificate> findByCertificateNo(String certificateNo);
}
