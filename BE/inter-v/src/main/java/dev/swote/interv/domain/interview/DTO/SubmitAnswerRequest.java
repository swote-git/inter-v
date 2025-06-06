package dev.swote.interv.domain.interview.DTO;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubmitAnswerRequest {
    @jakarta.validation.constraints.NotBlank(message = "답변은 필수입니다")
    private String content;

    private boolean useAIEvaluation = true;  // AI 평가 사용 여부
    private String resumeContent = "";       // 평가용 이력서
    private String coverLetter = "";         // 평가용 자기소개서
}