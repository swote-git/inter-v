package dev.swote.interv.domain.interview.DTO;

import dev.swote.interv.domain.interview.entity.InterviewMode;
import dev.swote.interv.domain.interview.entity.InterviewType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInterviewRequest {

    @NotNull(message = "이력서 ID는 필수입니다")
    private Integer resumeId;

    @NotNull(message = "포지션 ID는 필수입니다")
    private Integer positionId;

    private String title;
    private String description;

    // 면접 타입 (TECHNICAL, BEHAVIORAL, MIXED 등)
    @NotNull(message = "면접 타입은 필수입니다")
    private InterviewType type;

    // 면접 모드 (PRACTICE, REAL 등)
    @NotNull(message = "면접 모드는 필수입니다")
    private InterviewMode mode;

    // AI 질문 생성 사용 여부
    @Builder.Default
    private boolean useAI = true;

    // 생성할 질문 수
    @Min(value = 1, message = "최소 1개의 질문이 필요합니다")
    @Max(value = 20, message = "최대 20개의 질문까지 생성 가능합니다")
    private int questionCount = 5;

    // 기존 질문 ID 목록 (선택사항)
    private List<Integer> questionIds;

    // 예상 면접 시간 (분)
    private Integer expectedDurationMinutes;

    // 난이도 레벨 (1-5)
    @Min(value = 1, message = "난이도는 1 이상이어야 합니다")
    @Max(value = 5, message = "난이도는 5 이하여야 합니다")
    private Integer difficultyLevel = 3;

    // 특정 카테고리 필터 (선택사항)
    private String categoryFilter;

    // 공개 여부
    private boolean isPublic = false;
}