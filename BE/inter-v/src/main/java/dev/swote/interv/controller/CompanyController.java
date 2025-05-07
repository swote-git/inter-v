package dev.swote.interv.controller;

import dev.swote.interv.config.CommonResponse;
import dev.swote.interv.domain.company.entity.Company;
import dev.swote.interv.domain.position.entity.Position;
import dev.swote.interv.service.company.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping
    public ResponseEntity<CommonResponse<List<Company>>> getAllCompanies() {
        List<Company> companies = companyService.getAllCompanies();
        return ResponseEntity.ok(CommonResponse.ok(companies));
    }

    @GetMapping("/search")
    public ResponseEntity<CommonResponse<List<Company>>> searchCompanies(
            @RequestParam String keyword
    ) {
        List<Company> companies = companyService.searchCompanies(keyword);
        return ResponseEntity.ok(CommonResponse.ok(companies));
    }

    @GetMapping("/{companyId}")
    public ResponseEntity<CommonResponse<Company>> getCompany(
            @PathVariable Integer companyId
    ) {
        Company company = companyService.getCompanyById(companyId);
        return ResponseEntity.ok(CommonResponse.ok(company));
    }

    @PostMapping
    public ResponseEntity<CommonResponse<Company>> createCompany(
            @RequestBody CreateCompanyRequest request
    ) {
        Company company = companyService.createCompany(
                request.getName(),
                request.getIndustry(),
                request.getDescription()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.ok(company));
    }

    @GetMapping("/{companyId}/positions")
    public ResponseEntity<CommonResponse<List<Position>>> getCompanyPositions(
            @PathVariable Integer companyId
    ) {
        List<Position> positions = companyService.getCompanyPositions(companyId);
        return ResponseEntity.ok(CommonResponse.ok(positions));
    }

    @PostMapping("/{companyId}/positions")
    public ResponseEntity<CommonResponse<Position>> createPosition(
            @PathVariable Integer companyId,
            @RequestBody CreatePositionRequest request
    ) {
        Position position = companyService.createPosition(
                companyId,
                request.getTitle(),
                request.getDescription(),
                request.getRequiredSkills()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.ok(position));
    }

    public static class CreateCompanyRequest {
        private String name;
        private String industry;
        private String description;

        // Getters and setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getIndustry() {
            return industry;
        }

        public void setIndustry(String industry) {
            this.industry = industry;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public static class CreatePositionRequest {
        private String title;
        private String description;
        private List<String> requiredSkills;

        // Getters and setters
        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<String> getRequiredSkills() {
            return requiredSkills;
        }

        public void setRequiredSkills(List<String> requiredSkills) {
            this.requiredSkills = requiredSkills;
        }
    }
}