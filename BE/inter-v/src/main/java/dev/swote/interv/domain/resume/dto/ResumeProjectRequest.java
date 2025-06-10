// ResumeProjectRequest.java - ID 필드 추가
package dev.swote.interv.domain.resume.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "프로젝트 경험 요청 DTO")
public class ResumeProjectRequest {

    @JsonProperty("projectId")
    @Schema(
            description = "프로젝트 ID (수정시에만 사용, 신규 생성시에는 null)",
            example = "1"
    )
    private Integer id;

    @NotBlank(message = "프로젝트명은 필수입니다")
    @Size(max = 100, message = "프로젝트명은 100자 이하로 입력해주세요")
    @Schema(
            description = "프로젝트명",
            example = "온라인 쇼핑몰 플랫폼",
            required = true,
            maxLength = 100
    )
    private String projectName;

    @Size(max = 2000, message = "프로젝트 설명은 2000자 이하로 입력해주세요")
    @Schema(
            description = "프로젝트 상세 설명",
            example = "Spring Boot와 React를 활용한 B2C 온라인 쇼핑몰 개발",
            maxLength = 2000
    )
    private String description;

    @Schema(description = "프로젝트 시작일", example = "2023-01-01")
    private LocalDate startDate;

    @Schema(description = "프로젝트 종료일", example = "2023-06-30")
    private LocalDate endDate;

    @Schema(description = "현재 진행 중 여부", example = "false")
    private Boolean inProgress;
}


