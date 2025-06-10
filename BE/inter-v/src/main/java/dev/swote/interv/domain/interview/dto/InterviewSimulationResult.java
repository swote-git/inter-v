package dev.swote.interv.domain.interview.dto;

import dev.swote.interv.domain.interview.entity.AnswerEvaluation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "면접 시뮬레이션 결과 DTO")
public class InterviewSimulationResult {

    @Schema(
            description = "AI가 생성한 질문 목록",
            example = """
                [
                  "프로젝트에서 가장 어려웠던 기술적 문제는 무엇이었나요?",
                  "팀워크에서 중요하게 생각하는 가치는 무엇인가요?",
                  "향후 5년간의 커리어 계획을 말씀해주세요."
                ]
                """
    )
    private List<String> generatedQuestions;

    @Schema(
            description = "시뮬레이션에서 선택된 질문",
            example = "프로젝트에서 가장 어려웠던 기술적 문제는 무엇이었나요?"
    )
    private String selectedQuestion;

    @Schema(
            description = "사용자가 제출한 답변",
            example = """
                가장 어려웠던 문제는 대용량 데이터 처리 시 발생한 성능 이슈였습니다.
                이를 해결하기 위해 인덱스 최적화와 배치 처리 로직을 도입하여 해결했습니다.
                """
    )
    private String userAnswer;

    @Schema(description = "AI가 제공한 답변 평가 결과")
    private AnswerEvaluation evaluationResult;
}
