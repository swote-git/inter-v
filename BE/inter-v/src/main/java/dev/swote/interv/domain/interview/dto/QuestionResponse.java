package dev.swote.interv.domain.interview.dto;

import dev.swote.interv.domain.interview.entity.QuestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "면접 질문 정보 응답 DTO")
public class QuestionResponse {

    @Schema(description = "질문 고유 ID", example = "1")
    private Integer id;

    @Schema(
            description = "질문 내용",
            example = "본인의 가장 큰 강점과 약점에 대해 구체적인 사례와 함께 설명해주세요."
    )
    private String content;

    @Schema(
            description = "질문 유형",
            example = "PERSONALITY",
            allowableValues = {"PERSONALITY", "TECHNICAL", "PROJECT", "SITUATION"}
    )
    private QuestionType type;

    @Schema(description = "질문 순서", example = "1", minimum = "1")
    private Integer sequence;

    @Schema(description = "난이도 레벨", example = "3", minimum = "1", maximum = "5")
    private Integer difficultyLevel;

    @Schema(
            description = "질문 카테고리",
            example = "인성면접",
            allowableValues = {"기술면접", "인성면접", "경험면접", "상황면접"}
    )
    private String category;

    @Schema(
            description = "질문 세부 카테고리",
            example = "강점/약점"
    )
    private String subCategory;

    @Schema(description = "해당 질문에 대한 답변 정보 (답변한 경우에만 포함)")
    private AnswerResponse answer;
}
