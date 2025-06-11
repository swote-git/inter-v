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
@Schema(description = "답변과 평가 결과를 포함한 응답 DTO")
public class AnswerWithEvaluationResponse {

    @Schema(description = "제출된 답변 정보")
    private AnswerResponse answer;

    @Schema(description = "AI 평가 결과 (AI 평가를 사용한 경우에만 포함)")
    private AnswerEvaluationResponse evaluation;
}