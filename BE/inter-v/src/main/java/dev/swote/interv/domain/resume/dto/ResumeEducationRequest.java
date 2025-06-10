package dev.swote.interv.domain.resume.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "학력 사항 요청")
public class ResumeEducationRequest {

    @JsonProperty("educationId")
    @Schema(
            description = "학력 ID (수정시에만 사용, 신규 생성시에는 null)",
            example = "1"
    )
    private Integer id;

    @NotBlank(message = "학교구분은 필수입니다")
    @Size(max = 50, message = "학교구분은 50자 이하로 입력해주세요")
    @Schema(description = "학교구분", example = "대학교", required = true)
    private String schoolType;

    @NotBlank(message = "학교명은 필수입니다")
    @Size(max = 100, message = "학교명은 100자 이하로 입력해주세요")
    @Schema(description = "학교명", example = "경희대학교", required = true)
    private String schoolName;

    @Size(max = 100, message = "위치는 100자 이하로 입력해주세요")
    @Schema(description = "학교 위치", example = "경기도 용인시")
    private String location;

    @Size(max = 100, message = "전공은 100자 이하로 입력해주세요")
    @Schema(description = "전공", example = "컴퓨터공학과")
    private String major;

    @Schema(description = "입학일", example = "2022-03-01")
    private LocalDate enrollmentDate;

    @Schema(description = "졸업일", example = "2026-02-28")
    private LocalDate graduationDate;

    @Schema(description = "재학 중 여부", example = "true")
    private Boolean inProgress;

    @Size(max = 10, message = "학점은 10자 이하로 입력해주세요")
    @Schema(description = "학점", example = "3.8/4.5")
    private String gpa;
}