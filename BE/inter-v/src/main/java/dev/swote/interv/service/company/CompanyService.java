package dev.swote.interv.service.company;

import dev.swote.interv.domain.company.entity.Company;
import dev.swote.interv.domain.company.repository.CompanyRepository;
import dev.swote.interv.domain.position.entity.Position;
import dev.swote.interv.domain.position.repository.PositionRepository;
import dev.swote.interv.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final PositionRepository positionRepository;

    @Transactional(readOnly = true)
    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Company> searchCompanies(String keyword) {
        return companyRepository.findByNameContaining(keyword);
    }

    @Transactional(readOnly = true)
    public Company getCompanyById(Integer companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
    }

    @Transactional
    public Company createCompany(String name, String industry, String description) {
        Company company = Company.builder()
                .name(name)
                .industry(industry)
                .description(description)
                .build();

        return companyRepository.save(company);
    }

    @Transactional(readOnly = true)
    public List<Position> getCompanyPositions(Integer companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        return positionRepository.findByCompany(company);
    }

    @Transactional(readOnly = true)
    public List<Position> searchPositions(String keyword) {
        return positionRepository.findByTitleContaining(keyword);
    }

    @Transactional(readOnly = true)
    public Position getPositionById(Integer positionId) {
        return positionRepository.findById(positionId)
                .orElseThrow(() -> new ResourceNotFoundException("Position not found"));
    }

    @Transactional
    public Position createPosition(Integer companyId, String title, String description, List<String> requiredSkills) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        Set<String> skills = new HashSet<>(requiredSkills);

        Position position = Position.builder()
                .company(company)
                .title(title)
                .description(description)
                .requiredSkills(skills)
                .build();

        return positionRepository.save(position);
    }
}