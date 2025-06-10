package dev.swote.interv.controller;

import dev.swote.interv.config.CommonResponse;
import dev.swote.interv.domain.resume.dto.CreateResumeRequest;
import dev.swote.interv.domain.resume.dto.ResumeListResponse;
import dev.swote.interv.domain.resume.dto.ResumeResponse;
import dev.swote.interv.domain.resume.dto.UpdateResumeRequest;
import dev.swote.interv.interceptor.CurrentUser;
import dev.swote.interv.service.resume.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
@Tag(name = "이력서 관리", description = "이력서 생성, 조회, 수정, 삭제 API")
public class ResumeController {

    private final ResumeService resumeService;

    @GetMapping
    @Operation(
            summary = "내 이력서 목록 조회",
            description = "현재 로그인한 사용자의 이력서 목록을 페이징으로 조회합니다. 각 이력서의 요약 정보를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "이력서 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommonResponse.class),
                            examples = @ExampleObject(
                                    name = "이력서 목록 응답 예시",
                                    value = """
                    {
                      "status": 200,
                      "data": {
                        "content": [
                          {
                            "id": 1,
                            "user": {
                              "id": 1,
                              "userName": "홍길동",
                              "nickname": "개발자홍길동"
                            },
                            "title": "백엔드 개발자 이력서",
                            "objective": "성장하는 스타트업에서 백엔드 개발자로 일하고 싶습니다.",
                            "status": "ACTIVE",
                            "skills": ["Java", "Spring Boot", "MySQL"],
                            "projectCount": 3,
                            "certificationCount": 2,
                            "workExperienceCount": 2,
                            "educationCount": 1,
                            "createdAt": "2024-06-01T10:30:00",
                            "updatedAt": "2024-06-01T15:45:00"
                          }
                        ],
                        "totalElements": 5,
                        "totalPages": 1,
                        "size": 20,
                        "number": 0
                      }
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<CommonResponse<Page<ResumeListResponse>>> getMyResumes(
            @Parameter(hidden = true) CurrentUser currentUser,
            @PageableDefault(size = 20)
            @Parameter(
                    description = "페이징 정보",
                    example = "page=0&size=20&sort=createdAt,desc"
            ) Pageable pageable
    ) {
        Page<ResumeListResponse> resumes = resumeService.getUserResumes(currentUser.id(), pageable);
        return ResponseEntity.ok(CommonResponse.ok(resumes));
    }

    @GetMapping("/{resumeId}")
    @Operation(
            summary = "이력서 상세 조회",
            description = "이력서 ID로 이력서의 상세 정보를 조회합니다. 프로젝트, 경력, 학력, 자격증 등 모든 정보를 포함합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "이력서 상세 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResumeResponse.class),
                            examples = @ExampleObject(
                                    name = "이력서 상세 응답 예시",
                                    value = """
                    {
                      "status": 200,
                      "data": {
                        "id": 1,
                        "user": {
                          "id": 1,
                          "userName": "홍길동",
                          "nickname": "개발자홍길동",
                          "birthDate": "1990-01-01"
                        },
                        "title": "백엔드 개발자 이력서",
                        "content": "안녕하세요. 3년차 백엔드 개발자 홍길동입니다...",
                        "objective": "성장하는 스타트업에서 백엔드 개발자로 일하고 싶습니다.",
                        "status": "ACTIVE",
                        "skills": ["Java", "Spring Boot", "MySQL", "Docker"],
                        "projects": [],
                        "certifications": [],
                        "workExperiences": [],
                        "educations": [],
                        "createdAt": "2024-06-01T10:30:00",
                        "updatedAt": "2024-06-01T15:45:00"
                      }
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(responseCode = "404", description = "이력서를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<CommonResponse<ResumeResponse>> getResume(
            @Parameter(description = "조회할 이력서 ID", example = "1")
            @PathVariable Integer resumeId
    ) {
        ResumeResponse resume = resumeService.getResumeById(resumeId);
        return ResponseEntity.ok(CommonResponse.ok(resume));
    }

    @PostMapping
    @Operation(
            summary = "이력서 생성",
            description = "새로운 이력서를 생성합니다. 프로젝트, 경력, 학력, 자격증 정보도 함께 등록할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "이력서 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResumeResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<CommonResponse<ResumeResponse>> createResume(
            @Parameter(hidden = true) CurrentUser currentUser,
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "생성할 이력서 정보",
                    content = @Content(
                            schema = @Schema(implementation = CreateResumeRequest.class),
                            examples = @ExampleObject(
                                    name = "이력서 생성 요청 예시",
                                    value = """
                        {
                          "title": "백엔드 개발자 이력서",
                          "content": "안녕하세요. 3년차 백엔드 개발자 홍길동입니다.\\n\\nJava와 Spring Boot를 활용하여 웹 애플리케이션을 개발해왔습니다.",
                          "objective": "성장하는 스타트업에서 백엔드 개발자로 일하고 싶습니다.",
                          "skills": ["Java", "Spring Boot", "MySQL", "Redis", "Docker", "AWS"],
                          "projects": [
                            {
                              "projectName": "온라인 쇼핑몰 플랫폼",
                              "description": "Spring Boot와 React를 활용한 B2C 온라인 쇼핑몰 개발",
                              "startDate": "2023-01-01",
                              "endDate": "2023-06-30",
                              "inProgress": false
                            }
                          ],
                          "certifications": [
                            {
                              "certificationName": "정보처리기사",
                              "issuingOrganization": "한국산업인력공단",
                              "acquiredDate": "2022-05-15",
                              "noExpiry": true
                            }
                          ],
                          "workExperiences": [
                            {
                              "companyName": "(주)테크스타트업",
                              "position": "백엔드 개발자",
                              "department": "개발팀",
                              "location": "서울특별시 강남구",
                              "startDate": "2021-03-01",
                              "endDate": "2023-12-31",
                              "currentlyWorking": false,
                              "responsibilities": "Spring Boot 기반 RESTful API 개발",
                              "achievements": "기존 시스템 대비 API 응답시간 30% 개선"
                            }
                          ],
                          "educations": [
                            {
                              "schoolType": "대학교",
                              "schoolName": "한국대학교",
                              "location": "서울특별시 동대문구",
                              "major": "컴퓨터공학과",
                              "enrollmentDate": "2017-03-01",
                              "graduationDate": "2021-02-28",
                              "inProgress": false,
                              "gpa": "3.8/4.5"
                            }
                          ]
                        }
                        """
                            )
                    )
            ) CreateResumeRequest request
    ) {
        ResumeResponse resume = resumeService.createResume(currentUser.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.ok(resume));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "이력서 파일 업로드",
            description = "PDF 또는 DOCX 파일로 이력서를 업로드합니다. 파일에서 텍스트를 추출하여 이력서 내용으로 저장합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "이력서 파일 업로드 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResumeResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "지원하지 않는 파일 형식"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "413", description = "파일 크기 초과"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<CommonResponse<ResumeResponse>> uploadResume(
            @Parameter(hidden = true) CurrentUser currentUser,
            @Parameter(
                    description = "업로드할 이력서 파일 (PDF 또는 DOCX)",
                    content = @Content(mediaType = "multipart/form-data")
            )
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "이력서 제목", example = "홍길동_이력서_2024")
            @RequestParam("title") String title
    ) throws IOException {
        ResumeResponse resume = resumeService.uploadResumeFile(
                currentUser.id(),
                file,
                title
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.ok(resume));
    }

    @PutMapping("/{resumeId}")
    @Operation(
            summary = "이력서 수정",
            description = "기존 이력서의 정보를 수정합니다. 모든 하위 정보(프로젝트, 경력, 학력, 자격증)도 함께 업데이트됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "이력서 수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResumeResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "404", description = "이력서를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<CommonResponse<ResumeResponse>> updateResume(
            @Parameter(description = "수정할 이력서 ID", example = "1")
            @PathVariable Integer resumeId,
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "수정할 이력서 정보",
                    content = @Content(
                            schema = @Schema(implementation = UpdateResumeRequest.class)
                    )
            ) UpdateResumeRequest request
    ) {
        ResumeResponse resume = resumeService.updateResume(resumeId, request);
        return ResponseEntity.ok(CommonResponse.ok(resume));
    }

    @DeleteMapping("/{resumeId}")
    @Operation(
            summary = "이력서 삭제",
            description = "이력서를 소프트 삭제합니다. 실제로는 데이터베이스에서 삭제되지 않고 삭제 표시만 됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "이력서 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "이력서를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Void> deleteResume(
            @Parameter(description = "삭제할 이력서 ID", example = "1")
            @PathVariable Integer resumeId
    ) {
        resumeService.deleteResume(resumeId);
        return ResponseEntity.noContent().build();
    }
}