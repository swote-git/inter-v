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
@Schema(description = "이력서 생성 요청 DTO", example = """
        {
          "title": "백엔드 개발자 이력서",
          "content": "안녕하세요. 3년차 백엔드 개발자 홍길동입니다.",
          "objective": "성장하는 스타트업에서 백엔드 개발자로 일하고 싶습니다.",
          "skills": ["Java", "Spring Boot", "MySQL", "Docker"]
        }
        """)
public class CreateResumeRequest {

    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 100, message = "제목은 100자 이하로 입력해주세요")
    @Schema(
            description = "이력서 제목",
            example = "백엔드 개발자 이력서",
            required = true,
            maxLength = 100,
            minLength = 1
    )
    private String title;

    @NotBlank(message = "내용은 필수입니다")
    @Size(max = 10000, message = "내용은 10000자 이하로 입력해주세요")
    @Schema(
            description = "이력서 본문 내용. 자기소개, 경험, 역량 등을 자유롭게 작성",
            example = """
                안녕하세요. 3년차 백엔드 개발자 홍길동입니다.
                
                Java와 Spring Boot를 주력으로 하여 웹 애플리케이션을 개발해왔으며, 
                RESTful API 설계와 데이터베이스 최적화에 관심이 많습니다.
                
                최근에는 MSA 아키텍처와 Docker를 활용한 컨테이너 기반 개발에 
                집중하고 있습니다.
                """,
            required = true,
            maxLength = 10000,
            minLength = 10
    )
    private String content;

    @Size(max = 1000, message = "목표는 1000자 이하로 입력해주세요")
    @Schema(
            description = "희망 직무, 목표, 지원 동기 등",
            example = "성장하는 스타트업에서 백엔드 개발자로 일하며, 대용량 트래픽 처리 경험을 쌓고 싶습니다.",
            maxLength = 1000
    )
    private String objective;

    @Schema(
            description = "보유 기술 스택 목록 (중복 제거됨)",
            example = "[\"Java\", \"Spring Boot\", \"MySQL\", \"Redis\", \"Docker\", \"AWS\", \"Git\", \"JPA\"]",
            type = "array"
    )
    private Set<String> skills;

    @Valid
    @Schema(description = "프로젝트 경험 목록 (최대 20개)")
    private List<ResumeProjectRequest> projects;

    @Valid
    @Schema(description = "보유 자격증 목록 (최대 30개)")
    private List<ResumeCertificationRequest> certifications;

    @Valid
    @Schema(description = "경력 사항 목록 (최대 10개)")
    private List<ResumeWorkExperienceRequest> workExperiences;

    @Valid
    @Schema(description = "학력 사항 목록 (최대 5개)")
    private List<ResumeEducationRequest> educations;
}