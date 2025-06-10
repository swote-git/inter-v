package dev.swote.interv.domain.interview.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "면접 답변 제출 요청 DTO")
public class SubmitAnswerRequest {

    @jakarta.validation.constraints.NotBlank(message = "답변은 필수입니다")
    @Schema(
            description = "면접 질문에 대한 답변",
            example = """
                저의 가장 큰 강점은 문제 해결 능력입니다. 
                이전 프로젝트에서 성능 이슈가 발생했을 때, 프로파일링 도구를 활용하여 
                병목 지점을 찾아내고 쿼리 최적화를 통해 응답속도를 50% 개선한 경험이 있습니다.
                """,
            maxLength = 5000
    )
    private String content;

    @Schema(
            description = "AI 자동 평가 사용 여부",
            example = "true",
            defaultValue = "true"
    )
    private boolean useAIEvaluation = true;

    @Schema(
            description = "평가에 참고할 이력서 내용",
            example = "3년차 백엔드 개발자, Spring Boot 전문가...",
            maxLength = 10000
    )
    private String resumeContent = "";

    @Schema(
            description = "평가에 참고할 자기소개서 내용",
            example = "저는 항상 새로운 기술을 학습하며 성장하는 개발자입니다...",
            maxLength = 3000
    )
    private String coverLetter = "";
}