package dev.swote.interv.domain.interview.repository;

import dev.swote.interv.domain.interview.entity.InterviewSession;
import dev.swote.interv.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Integer> {
    List<InterviewSession> findByUser(User user);
    Page<InterviewSession> findByUser(User user, Pageable pageable);
    Optional<InterviewSession> findByShareUrl(String shareUrl);
}