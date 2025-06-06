package dev.swote.interv.domain.interview.DTO;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SimulateInterviewRequest {
    @jakarta.validation.constraints.NotBlank(message = "이력서 내용은 필수입니다")
    private String resumeContent;

    private String coverLetter = "";

    @jakarta.validation.constraints.NotBlank(message = "채용공고는 필수입니다")
    private String jobDescription;

    @jakarta.validation.constraints.NotBlank(message = "답변은 필수입니다")
    private String userAnswer;

    @jakarta.validation.constraints.Min(value = 1, message = "최소 1개의 질문이 필요합니다")
    @jakarta.validation.constraints.Max(value = 10, message = "최대 10개의 질문까지 생성 가능합니다")
    private int numQuestions = 3;
}
