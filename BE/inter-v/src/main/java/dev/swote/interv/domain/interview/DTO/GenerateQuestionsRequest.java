package dev.swote.interv.domain.interview.DTO;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GenerateQuestionsRequest {
    @jakarta.validation.constraints.NotBlank(message = "이력서 내용은 필수입니다")
    private String resumeContent;

    @jakarta.validation.constraints.NotBlank(message = "포지션은 필수입니다")
    private String position;

    @jakarta.validation.constraints.Min(value = 1, message = "최소 1개의 질문이 필요합니다")
    @jakarta.validation.constraints.Max(value = 20, message = "최대 20개의 질문까지 생성 가능합니다")
    private int questionCount = 5;
}