package dev.swote.interv.domain.interview.repository;

import dev.swote.interv.domain.interview.entity.InterviewSimulation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InterviewSimulationRepository extends JpaRepository<InterviewSimulation, Integer> {
}
