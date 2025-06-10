package dev.swote.interv.domain.resume.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "학력 사항 응답")
public class ResumeEducationResponse {

    @Schema(description = "학력 ID", example = "1")
    private Integer id;

    @Schema(description = "학교구분", example = "대학교")
    private String schoolType;

    @Schema(description = "학교명", example = "한국대학교")
    private String schoolName;

    @Schema(description = "학교 위치", example = "서울특별시 동대문구")
    private String location;

    @Schema(description = "전공", example = "컴퓨터공학과")
    private String major;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "입학일", example = "2017-03-01")
    private LocalDate enrollmentDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "졸업일", example = "2021-02-28")
    private LocalDate graduationDate;

    @Schema(description = "재학 중 여부", example = "false")
    private Boolean inProgress;

    @Schema(description = "학점", example = "3.8/4.5")
    private String gpa;
}