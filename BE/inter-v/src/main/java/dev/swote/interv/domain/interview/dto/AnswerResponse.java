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
@Schema(description = "면접 답변 정보 응답 DTO")
public class AnswerResponse {

    @Schema(description = "답변 고유 ID", example = "1")
    private Integer id;

    @Schema(
            description = "답변 내용",
            example = """
                저의 가장 큰 강점은 문제 해결 능력입니다. 
                이전 프로젝트에서 성능 이슈 발생 시 프로파일링 도구를 활용하여 
                병목 지점을 찾아내고 쿼리 최적화로 응답속도를 50% 개선했습니다.
                """
    )
    private String content;

    @Schema(
            description = "AI가 제공한 피드백",
            example = """
                구체적인 사례와 수치를 들어 설명한 점이 좋습니다. 
                다만 약점에 대한 언급과 개선 노력도 함께 설명하면 더욱 완성도 높은 답변이 될 것입니다.
                """
    )
    private String feedback;

    @Schema(
            description = "음성 답변 파일 경로 (음성 면접인 경우)",
            example = "/audio/answers/20240601/answer_1234567890.wav"
    )
    private String audioFilePath;

    @Schema(description = "의사소통 점수 (1-10)", example = "8", minimum = "1", maximum = "10")
    private Integer communicationScore;

    @Schema(description = "기술적 정확성 점수 (1-10)", example = "9", minimum = "1", maximum = "10")
    private Integer technicalScore;

    @Schema(description = "답변 구조화 점수 (1-10)", example = "7", minimum = "1", maximum = "10")
    private Integer structureScore;
}