package dev.swote.interv.domain.resume.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "이력서 수정 요청")
public class UpdateResumeRequest {

    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 100, message = "제목은 100자 이하로 입력해주세요")
    @Schema(description = "이력서 제목", example = "백엔드 개발자 이력서 (수정)", required = true)
    private String title;

    @NotBlank(message = "내용은 필수입니다")
    @Size(max = 10000, message = "내용은 10000자 이하로 입력해주세요")
    @Schema(description = "이력서 내용",
            example = "안녕하세요. 4년차 백엔드 개발자 홍길동입니다.\n\n최근에는 MSA 아키텍처와 Kubernetes를 활용한 컨테이너 오케스트레이션에 관심을 가지고 있습니다.",
            required = true)
    private String content;

    @Size(max = 1000, message = "목표는 1000자 이하로 입력해주세요")
    @Schema(description = "목표/희망사항",
            example = "글로벌 서비스를 운영하는 회사에서 시니어 백엔드 개발자로 성장하고 싶습니다.")
    private String objective;

    @Schema(description = "보유 기술 목록",
            example = "[\"Java\", \"Spring Boot\", \"Kubernetes\", \"MySQL\", \"Redis\", \"Docker\", \"AWS\", \"Git\"]")
    private Set<String> skills;

    @Valid
    @Schema(description = "프로젝트 경험 목록")
    private List<ResumeProjectRequest> projects;

    @Valid
    @Schema(description = "자격증 목록")
    private List<ResumeCertificationRequest> certifications;

    @Valid
    @Schema(description = "경력 사항 목록")
    private List<ResumeWorkExperienceRequest> workExperiences;

    @Valid
    @Schema(description = "학력 사항 목록")
    private List<ResumeEducationRequest> educations;
}