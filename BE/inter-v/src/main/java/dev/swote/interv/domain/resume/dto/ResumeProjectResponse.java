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
@Schema(description = "프로젝트 경험 응답 DTO")
public class ResumeProjectResponse {

    @Schema(description = "프로젝트 고유 ID", example = "1")
    private Integer id;

    @Schema(description = "프로젝트명", example = "온라인 쇼핑몰 플랫폼")
    private String projectName;

    @Schema(
            description = "프로젝트 상세 설명",
            example = "Spring Boot와 React를 활용한 B2C 온라인 쇼핑몰 개발. 결제 시스템 연동, 상품 관리, 주문 처리 기능을 담당했습니다."
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
            description = "프로젝트 종료일",
            example = "2023-06-30",
            type = "string",
            format = "date"
    )
    private LocalDate endDate;

    @Schema(description = "진행 중 여부", example = "false")
    private Boolean inProgress;
}
