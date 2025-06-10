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
@Schema(description = "이력서 요약 정보 DTO")
public class ResumeSimpleResponse {

    @Schema(description = "이력서 고유 ID", example = "1")
    private Integer id;

    @Schema(description = "이력서 제목", example = "백엔드 개발자 이력서")
    private String title;

    @Schema(
            description = "목표/희망사항",
            example = "성장하는 스타트업에서 백엔드 개발자로 일하고 싶습니다."
    )
    private String objective;
}