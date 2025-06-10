package dev.swote.interv.domain.resume.dto;

import dev.swote.interv.domain.resume.entity.ResumeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "이력서 상세 정보 응답 DTO")
public class ResumeResponse {

    @Schema(description = "이력서 고유 ID", example = "1")
    private Integer id;

    @Schema(description = "작성자 정보")
    private UserSimpleResponse user;

    @Schema(description = "이력서 제목", example = "백엔드 개발자 이력서")
    private String title;

    @Schema(
            description = "이력서 본문 내용",
            example = """
                안녕하세요. 3년차 백엔드 개발자 홍길동입니다.
                
                Java와 Spring Boot를 활용하여 웹 애플리케이션을 개발해왔으며, 
                RESTful API 설계와 데이터베이스 최적화에 관심이 많습니다.
                """
    )
    private String content;

    @Schema(
            description = "목표/희망사항",
            example = "성장하는 스타트업에서 백엔드 개발자로 일하며, 대용량 트래픽 처리 경험을 쌓고 싶습니다."
    )
    private String objective;

    @Schema(
            description = "업로드된 파일의 저장 경로 (파일 업로드로 생성된 경우)",
            example = "/files/resume/2024/06/01/resume_1234567890.pdf"
    )
    private String filePath;

    @Schema(
            description = "이력서 상태",
            example = "ACTIVE",
            allowableValues = {"DRAFT", "ACTIVE", "ARCHIVED"}
    )
    private ResumeStatus status;

    @Schema(
            description = "보유 기술 스택 목록",
            example = "[\"Java\", \"Spring Boot\", \"MySQL\", \"Redis\", \"Docker\", \"AWS\"]"
    )
    private Set<String> skills;

    @Schema(description = "프로젝트 경험 목록")
    private List<ResumeProjectResponse> projects;

    @Schema(description = "보유 자격증 목록")
    private List<ResumeCertificationResponse> certifications;

    @Schema(description = "경력 사항 목록")
    private List<ResumeWorkExperienceResponse> workExperiences;

    @Schema(description = "학력 사항 목록")
    private List<ResumeEducationResponse> educations;

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