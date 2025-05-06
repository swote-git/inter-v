package dev.swote.interv.domain.interview.repository;

import dev.swote.interv.domain.interview.entity.InterviewSession;
import dev.swote.interv.domain.interview.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Integer> {
    List<Question> findByInterviewSessionOrderBySequenceAsc(InterviewSession interviewSession);
}