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
@Schema(description = "회사 요약 정보 DTO")
public class CompanySimpleResponse {

    @Schema(description = "회사 고유 ID", example = "1")
    private Integer id;

    @Schema(description = "회사명", example = "(주)테크스타트업")
    private String name;

    @Schema(
            description = "업종/산업 분야",
            example = "IT/소프트웨어",
            allowableValues = {"IT/소프트웨어", "제조업", "금융업", "교육", "의료", "서비스업", "기타"}
    )
    private String industry;
}