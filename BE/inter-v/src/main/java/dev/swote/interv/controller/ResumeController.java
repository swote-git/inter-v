package dev.swote.interv.controller;

import dev.swote.interv.config.CommonResponse;
import dev.swote.interv.domain.resume.dto.CreateResumeRequest;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/resume")  // 단수형으로 변경
@RequiredArgsConstructor
@Tag(name = "이력서 관리", description = "이력서 생성, 조회, 수정, 삭제 API (1:1 관계)")
public class ResumeController {

    private final ResumeService resumeService;

    @GetMapping
    @Operation(
            summary = "내 이력서 조회",
            description = "현재 로그인한 사용자의 이력서를 조회합니다. 이력서가 없는 경우 404를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "이력서 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommonResponse.class),
                            examples = @ExampleObject(
                                    name = "이력서 응답 예시",
                                    value = """
                    {
                      "status": 200,
                      "data": {
                        "id": 1,
                        "user": {
                          "id": 1,
                          "userName": "홍길동",
                          "nickname": "개발자홍길동"
                        },
                        "title": "백엔드 개발자 이력서",
                        "content": "안녕하세요. 3년차 백엔드 개발자 홍길동입니다...",
                        "objective": "성장하는 스타트업에서 백엔드 개발자로 일하고 싶습니다.",
                        "status": "ACTIVE",
                        "skills": ["Java", "Spring Boot", "MySQL"],
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
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<CommonResponse<ResumeResponse>> getMyResume(
            @Parameter(hidden = true) CurrentUser currentUser
    ) {
        ResumeResponse resume = resumeService.getUserResume(currentUser.id());
        return ResponseEntity.ok(CommonResponse.ok(resume));
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
                            schema = @Schema(implementation = ResumeResponse.class)
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
            description = "새로운 이력서를 생성합니다. 한 사용자당 하나의 이력서만 생성 가능합니다."
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
            @ApiResponse(responseCode = "409", description = "이미 이력서가 존재함"),
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
                          "content": "안녕하세요. 3년차 백엔드 개발자 홍길동입니다.",
                          "objective": "성장하는 스타트업에서 백엔드 개발자로 일하고 싶습니다.",
                          "skills": ["Java", "Spring Boot", "MySQL", "Docker"],
                          "projects": [...],
                          "certifications": [...],
                          "workExperiences": [...],
                          "educations": [...]
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
            description = "PDF 또는 DOCX 파일로 이력서를 업로드합니다. 한 사용자당 하나의 이력서만 생성 가능합니다."
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
            @ApiResponse(responseCode = "409", description = "이미 이력서가 존재함"),
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

    @PutMapping
    @Operation(
            summary = "내 이력서 수정",
            description = "현재 로그인한 사용자의 이력서를 수정합니다."
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
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<CommonResponse<ResumeResponse>> updateMyResume(
            @Parameter(hidden = true) CurrentUser currentUser,
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "수정할 이력서 정보",
                    content = @Content(
                            schema = @Schema(implementation = UpdateResumeRequest.class)
                    )
            ) UpdateResumeRequest request
    ) {
        ResumeResponse resume = resumeService.updateUserResume(currentUser.id(), request);
        return ResponseEntity.ok(CommonResponse.ok(resume));
    }

    @DeleteMapping
    @Operation(
            summary = "내 이력서 삭제",
            description = "현재 로그인한 사용자의 이력서를 소프트 삭제합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "이력서 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "이력서를 찾을 수 없음"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Void> deleteMyResume(
            @Parameter(hidden = true) CurrentUser currentUser
    ) {
        resumeService.deleteUserResume(currentUser.id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/exists")
    @Operation(
            summary = "이력서 존재 여부 확인",
            description = "현재 로그인한 사용자의 이력서 존재 여부를 확인합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "확인 완료"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    public ResponseEntity<CommonResponse<Boolean>> checkResumeExists(
            @Parameter(hidden = true) CurrentUser currentUser
    ) {
        boolean exists = resumeService.existsUserResume(currentUser.id());
        return ResponseEntity.ok(CommonResponse.ok(exists));
    }
}