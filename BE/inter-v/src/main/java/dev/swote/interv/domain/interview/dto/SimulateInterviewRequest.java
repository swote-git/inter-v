package dev.swote.interv.domain.interview.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "면접 시뮬레이션 요청 DTO")
public class SimulateInterviewRequest {

    @jakarta.validation.constraints.NotBlank(message = "이력서 내용은 필수입니다")
    @Schema(
            description = "시뮬레이션에 사용할 이력서 내용",
            example = """
                3년차 백엔드 개발자로 Java, Spring Boot, MySQL을 주력으로 사용합니다.
                RESTful API 개발과 데이터베이스 설계 경험이 풍부합니다.
                """,
            required = true,
            maxLength = 10000
    )
    private String resumeContent;

    @Schema(
            description = "자기소개서 내용 (선택사항)",
            example = """
                안녕하세요. 끊임없이 학습하고 성장하는 개발자가 되고 싶은 홍길동입니다.
                새로운 기술에 대한 호기심과 문제 해결에 대한 열정을 가지고 있습니다.
                """,
            maxLength = 3000
    )
    private String coverLetter = "";

    @jakarta.validation.constraints.NotBlank(message = "채용공고는 필수입니다")
    @Schema(
            description = "지원하는 회사의 채용공고 내용",
            example = """
                [백엔드 개발자 채용]
                • 자격요건: Java, Spring Framework 3년 이상 경험
                • 우대사항: 대용량 트래픽 처리 경험, 클라우드 서비스 활용 경험
                • 담당업무: 서비스 백엔드 API 개발, 데이터베이스 설계 및 최적화
                """,
            required = true,
            maxLength = 5000
    )
    private String jobDescription;

    @jakarta.validation.constraints.NotBlank(message = "답변은 필수입니다")
    @Schema(
            description = "시뮬레이션할 사용자 답변",
            example = """
                저는 3년간 Spring Boot를 활용한 백엔드 개발 경험이 있습니다.
                특히 대용량 데이터 처리와 API 성능 최적화에 관심이 많으며,
                이전 프로젝트에서 Redis 캐시를 도입하여 응답속도를 개선한 경험이 있습니다.
                """,
            required = true,
            maxLength = 5000
    )
    private String userAnswer;

    @jakarta.validation.constraints.Min(value = 1, message = "최소 1개의 질문이 필요합니다")
    @jakarta.validation.constraints.Max(value = 10, message = "최대 10개의 질문까지 생성 가능합니다")
    @Schema(
            description = "시뮬레이션에서 생성할 질문 개수",
            example = "3",
            minimum = "1",
            maximum = "10",
            defaultValue = "3"
    )
    private int numQuestions = 3;
}