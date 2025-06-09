package dev.swote.interv.domain.resume.dto;

import dev.swote.interv.domain.resume.entity.ResumeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "이력서 목록용 요약 정보 응답 DTO")
public class ResumeListResponse {

    @Schema(description = "이력서 고유 ID", example = "1")
    private Integer id;

    @Schema(description = "작성자 기본 정보")
    private UserSimpleResponse user;

    @Schema(description = "이력서 제목", example = "백엔드 개발자 이력서")
    private String title;

    @Schema(
            description = "목표/희망사항 요약",
            example = "성장하는 스타트업에서 백엔드 개발자로 일하고 싶습니다."
    )
    private String objective;

    @Schema(
            description = "이력서 상태",
            example = "ACTIVE",
            allowableValues = {"DRAFT", "ACTIVE", "ARCHIVED"}
    )
    private ResumeStatus status;

    @Schema(
            description = "보유 기술 스택 목록",
            example = "[\"Java\", \"Spring Boot\", \"MySQL\", \"Docker\"]"
    )
    private Set<String> skills;

    @Schema(description = "등록된 프로젝트 개수", example = "3", minimum = "0")
    private int projectCount;

    @Schema(description = "보유 자격증 개수", example = "2", minimum = "0")
    private int certificationCount;

    @Schema(description = "경력 사항 개수", example = "2", minimum = "0")
    private int workExperienceCount;

    @Schema(description = "학력 사항 개수", example = "1", minimum = "0")
    private int educationCount;

    @Schema(
            description = "이력서 생성일시",
            example = "2024-06-01T10:30:00",
            type = "string",
            format = "date-time"
    )
    private LocalDateTime createdAt;

    @Schema(
            description = "마지막 수정일시",
            example = "2024-06-01T15:45:00",
            type = "string",
            format = "date-time"
    )
    private LocalDateTime updatedAt;
}
