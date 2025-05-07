package dev.swote.interv.domain.resume.repository;

import dev.swote.interv.domain.resume.entity.ResumeProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResumeProjectRepository extends JpaRepository<ResumeProject, Integer> {
    List<ResumeProject> findByResumeId(Integer resumeId);
}