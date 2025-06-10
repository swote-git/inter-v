package dev.swote.interv.domain.position.repository;

import dev.swote.interv.domain.company.entity.Company;
import dev.swote.interv.domain.position.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PositionRepository extends JpaRepository<Position, Integer> {
    List<Position> findByCompany(Company company);
    List<Position> findByTitleContaining(String title);
}