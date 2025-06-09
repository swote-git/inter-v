package dev.swote.interv.domain.interview.dto;

import dev.swote.interv.domain.interview.entity.InterviewMode;
import dev.swote.interv.domain.interview.entity.InterviewType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Data
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "면접 세션 생성 요청 DTO")
public class CreateInterviewRequest {

    @NotNull(message = "이력서 ID는 필수입니다")
    @Schema(
            description = "면접에 사용할 이력서 ID",
            example = "1",
            required = true
    )
    private Integer resumeId;

    @NotNull(message = "포지션 ID는 필수입니다")
    @Schema(
            description = "지원하고자 하는 포지션 ID",
            example = "1",
            required = true
    )
    private Integer positionId;

    @Schema(
            description = "면접 세션 제목 (선택사항, 자동 생성됨)",
            example = "백엔드 개발자 포지션 면접 연습",
            maxLength = 200
    )
    private String title;

    @Schema(
            description = "면접에 대한 추가 설명이나 메모",
            example = "Spring Boot 관련 질문 위주로 연습하고 싶습니다.",
            maxLength = 1000
    )
    private String description;

    @NotNull(message = "면접 타입은 필수입니다")
    @Schema(
            description = "면접 방식",
            example = "TEXT",
            allowableValues = {"TEXT", "VOICE"},
            required = true
    )
    private InterviewType type;

    @NotNull(message = "면접 모드는 필수입니다")
    @Schema(
            description = "면접 모드",
            example = "PRACTICE",
            allowableValues = {"PRACTICE", "REAL"},
            required = true
    )
    private InterviewMode mode;

    @Builder.Default
    @Schema(
            description = "AI 질문 자동 생성 사용 여부",
            example = "true",
            defaultValue = "true"
    )
    private boolean useAI = true;

    @Min(value = 1, message = "최소 1개의 질문이 필요합니다")
    @Max(value = 20, message = "최대 20개의 질문까지 생성 가능합니다")
    @Schema(
            description = "생성할 질문 개수",
            example = "5",
            minimum = "1",
            maximum = "20",
            defaultValue = "5"
    )
    private int questionCount = 5;

    @Schema(
            description = "기존 질문 ID 목록 (선택사항, 지정된 질문들로 면접 구성)",
            example = "[1, 5, 10, 15]"
    )
    private List<Integer> questionIds;

    @Schema(
            description = "예상 면접 시간 (분)",
            example = "30",
            minimum = "5",
            maximum = "180"
    )
    private Integer expectedDurationMinutes;

    @Min(value = 1, message = "난이도는 1 이상이어야 합니다")
    @Max(value = 5, message = "난이도는 5 이하여야 합니다")
    @Schema(
            description = "질문 난이도 레벨",
            example = "3",
            minimum = "1",
            maximum = "5",
            defaultValue = "3"
    )
    private Integer difficultyLevel = 3;

    @Schema(
            description = "특정 카테고리 필터 (선택사항)",
            example = "기술면접",
            allowableValues = {"기술면접", "인성면접", "경험면접", "상황면접"}
    )
    private String categoryFilter;

    @Schema(
            description = "면접 결과 공개 여부",
            example = "false",
            defaultValue = "false"
    )
    private boolean isPublic = false;
}