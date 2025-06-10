package dev.swote.interv.domain.resume.dto;

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
            description = "프로젝트 상세 설명. 사용 기술, 담당 역할, 성과 등을 포함",
            example = """
                Spring Boot와 React를 활용한 B2C 온라인 쇼핑몰 개발 프로젝트입니다.
                
                • 담당 역할: 백엔드 API 개발, 결제 시스템 연동
                • 사용 기술: Spring Boot, MySQL, Redis, AWS EC2
                • 주요 성과: 월 평균 1만 건의 주문 처리, 결제 성공률 99.5% 달성
                """,
            maxLength = 2000
    )
    private String description;

    @Schema(
            description = "프로젝트 시작일",
            example = "2023-01-01",
            type = "string",
            format = "date"
    )
    private LocalDate startDate;

    @Schema(
            description = "프로젝트 종료일 (진행 중인 경우 null)",
            example = "2023-06-30",
            type = "string",
            format = "date"
    )
    private LocalDate endDate;

    @Schema(
            description = "현재 진행 중 여부",
            example = "false",
            defaultValue = "false"
    )
    private Boolean inProgress;
}