package dev.swote.interv.domain.resume.repository;

import dev.swote.interv.domain.resume.entity.Resume;
import dev.swote.interv.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Integer> {
    List<Resume> findByUser(User user);
    Page<Resume> findByUser(User user, Pageable pageable);
}