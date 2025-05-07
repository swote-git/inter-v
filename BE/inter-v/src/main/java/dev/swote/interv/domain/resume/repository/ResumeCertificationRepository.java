package dev.swote.interv.domain.resume.repository;

import dev.swote.interv.domain.resume.entity.ResumeCertification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResumeCertificationRepository extends JpaRepository<ResumeCertification, Integer> {
    List<ResumeCertification> findByResumeId(Integer resumeId);
}