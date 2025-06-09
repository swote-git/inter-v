package dev.swote.interv.domain.interview.dto;

import dev.swote.interv.domain.interview.entity.InterviewMode;
import dev.swote.interv.domain.interview.entity.InterviewStatus;
import dev.swote.interv.domain.interview.entity.InterviewType;
import dev.swote.interv.domain.resume.dto.UserSimpleResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "면접 세션 목록용 요약 정보 응답 DTO")
public class InterviewListResponse {

    @Schema(description = "면접 세션 고유 ID", example = "1")
    private Integer id;

    @Schema(description = "면접 참가자 정보")
    private UserSimpleResponse user;

    @Schema(description = "사용된 이력서 요약 정보")
    private ResumeSimpleResponse resume;

    @Schema(description = "지원 포지션 요약 정보")
    private PositionSimpleResponse position;

    @Schema(
            description = "면접 방식",
            example = "TEXT",
            allowableValues = {"TEXT", "VOICE"}
    )
    private InterviewType type;

    @Schema(
            description = "면접 모드",
            example = "PRACTICE",
            allowableValues = {"PRACTICE", "REAL"}
    )
    private InterviewMode mode;

    @Schema(
            description = "면접 진행 상태",
            example = "COMPLETED",
            allowableValues = {"SCHEDULED", "IN_PROGRESS", "COMPLETED", "ABANDONED"}
    )
    private InterviewStatus status;

    @Schema(
            description = "면접 시작 시간",
            example = "2024-06-01T14:30:00",
            type = "string",
            format = "date-time"
    )
    private LocalDateTime startTime;

    @Schema(
            description = "면접 종료 시간",
            example = "2024-06-01T15:15:00",
            type = "string",
            format = "date-time"
    )
    private LocalDateTime endTime;

    @Schema(description = "전체 질문 개수", example = "5", minimum = "1")
    private Integer questionCount;

    @Schema(description = "답변 완료한 질문 개수", example = "5", minimum = "0")
    private Integer answeredQuestionCount;

    @Schema(
            description = "면접 세션 생성일시",
            example = "2024-06-01T14:00:00",
            type = "string",
            format = "date-time"
    )
    private LocalDateTime createdAt;
}