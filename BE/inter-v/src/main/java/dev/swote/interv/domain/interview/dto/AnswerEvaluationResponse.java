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
@Schema(description = "답변 평가 결과 응답 DTO")
public class AnswerEvaluationResponse {

    @Schema(description = "평가 고유 ID", example = "1")
    private Integer id;

    @Schema(description = "관련성 점수 (1-10)", example = "8", minimum = "1", maximum = "10")
    private Integer relevance;

    @Schema(description = "구체성 점수 (1-10)", example = "7", minimum = "1", maximum = "10")
    private Integer specificity;

    @Schema(description = "실무성 점수 (1-10)", example = "9", minimum = "1", maximum = "10")
    private Integer practicality;

    @Schema(description = "유효성 점수 (1-10)", example = "8", minimum = "1", maximum = "10")
    private Integer validity;

    @Schema(description = "총점 (4-40)", example = "32", minimum = "4", maximum = "40")
    private Integer totalScore;

    @Schema(
            description = "AI가 제공한 상세 피드백",
            example = """
                전반적으로 우수한 답변입니다. 구체적인 사례를 제시하여 답변의 신뢰성을 높였습니다.
                다만 향후 개선 계획에 대한 언급이 추가되면 더욱 완성도 높은 답변이 될 것입니다.
                """
    )
    private String feedback;

    @Schema(
            description = "평가 유형",
            example = "AI_FASTAPI",
            allowableValues = {"AI", "HUMAN", "AUTO", "AI_FASTAPI"}
    )
    private String evaluationType;
}