package dev.swote.interv.domain.resume.repository;

import dev.swote.interv.domain.resume.entity.ResumeWorkExperience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResumeWorkExperienceRepository extends JpaRepository<ResumeWorkExperience, Integer> {
    List<ResumeWorkExperience> findByResumeId(Integer resumeId);
}