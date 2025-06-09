package dev.swote.interv.domain.interview.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "AI 질문 생성 요청 DTO")
public class GenerateQuestionsRequest {

    @jakarta.validation.constraints.NotBlank(message = "이력서 내용은 필수입니다")
    @Schema(
            description = "질문 생성에 사용할 이력서 내용",
            example = """
                3년차 백엔드 개발자입니다. Java, Spring Boot를 주력으로 사용하며, 
                RESTful API 개발과 데이터베이스 최적화 경험이 있습니다.
                """,
            required = true,
            maxLength = 10000
    )
    private String resumeContent;

    @jakarta.validation.constraints.NotBlank(message = "포지션은 필수입니다")
    @Schema(
            description = "지원 포지션명",
            example = "백엔드 개발자",
            required = true,
            maxLength = 100
    )
    private String position;

    @jakarta.validation.constraints.Min(value = 1, message = "최소 1개의 질문이 필요합니다")
    @jakarta.validation.constraints.Max(value = 20, message = "최대 20개의 질문까지 생성 가능합니다")
    @Schema(
            description = "생성할 질문 개수",
            example = "5",
            minimum = "1",
            maximum = "20",
            defaultValue = "5"
    )
    private int questionCount = 5;
}