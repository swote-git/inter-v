package dev.swote.interv.domain.interview.repository;

import dev.swote.interv.domain.interview.entity.AnswerEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnswerEvaluationRepository extends JpaRepository<AnswerEvaluation, Integer> {
}
