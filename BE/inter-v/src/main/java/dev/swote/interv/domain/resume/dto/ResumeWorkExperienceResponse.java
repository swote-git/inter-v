package dev.swote.interv.domain.resume.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "경력 사항 응답")
public class ResumeWorkExperienceResponse {

    @Schema(description = "경력 ID", example = "1")
    private Integer id;

    @Schema(description = "회사명", example = "(주)테크스타트업")
    private String companyName;

    @Schema(description = "직책/직급", example = "백엔드 개발자")
    private String position;

    @Schema(description = "부서명", example = "개발팀")
    private String department;

    @Schema(description = "근무지", example = "서울특별시 강남구")
    private String location;

    @Schema(description = "입사일", example = "2021-03-01")
    private LocalDate startDate;

    @Schema(description = "퇴사일", example = "2023-12-31")
    private LocalDate endDate;

    @Schema(description = "현재 재직 중 여부", example = "false")
    private Boolean currentlyWorking;

    @Schema(description = "담당업무")
    private String responsibilities;

    @Schema(description = "주요 성과")
    private String achievements;
}