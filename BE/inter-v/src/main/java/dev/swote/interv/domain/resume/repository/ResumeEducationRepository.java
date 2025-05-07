package dev.swote.interv.domain.resume.repository;

import dev.swote.interv.domain.resume.entity.ResumeEducation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResumeEducationRepository extends JpaRepository<ResumeEducation, Integer> {
    List<ResumeEducation> findByResumeId(Integer resumeId);
}