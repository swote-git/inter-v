
package dev.swote.interv.domain.resume.repository;

import dev.swote.interv.domain.resume.entity.Resume;
import dev.swote.interv.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Integer> {
    Optional<Resume> findByUser(User user);

    Optional<Resume> findByUserId(Integer userId);

    boolean existsByUserId(Integer userId);
}