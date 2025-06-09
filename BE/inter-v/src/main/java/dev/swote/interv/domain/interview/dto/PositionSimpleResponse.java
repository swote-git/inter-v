package dev.swote.interv.domain.interview.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "포지션 요약 정보 DTO")
public class PositionSimpleResponse {

    @Schema(description = "포지션 고유 ID", example = "1")
    private Integer id;

    @Schema(description = "포지션명", example = "백엔드 개발자")
    private String name;

    @Schema(description = "포지션 제목", example = "주니어/시니어 백엔드 개발자")
    private String title;

    @Schema(description = "소속 회사 정보")
    private CompanySimpleResponse company;
}