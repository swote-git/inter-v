package dev.swote.interv.controller;

import dev.swote.interv.config.CommonResponse;
import dev.swote.interv.domain.interview.dto.*;
import dev.swote.interv.domain.interview.entity.*;
import dev.swote.interv.interceptor.CurrentUser;
import dev.swote.interv.service.ai.MLIntegrationService;
import dev.swote.interv.service.interview.InterviewService;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
@Tag(name = "면접 관리", description = "AI 기반 면접 연습 및 관리 API")
public class InterviewController {

    private final InterviewService interviewService;
    private final MLIntegrationService mlIntegrationService;

    @GetMapping
    @Operation(
            summary = "내 면접 목록 조회",
            description = "현재 사용자가 진행한 면접 세션 목록을 페이징으로 조회합니다. 면접 상태, 진행도, 소요 시간 등의 요약 정보를 제공합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "면접 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommonResponse.class),
                            examples = @ExampleObject(
                                    name = "면접 목록 응답 예시",
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
                            "resume": {
                              "id": 1,
                              "title": "백엔드 개발자 이력서",
                              "objective": "성장하는 스타트업에서 일하고 싶습니다."
                            },
                            "position": {
                              "id": 1,
                              "name": "백엔드 개발자",
                              "title": "주니어 백엔드 개발자",
                              "company": {
                                "id": 1,
                                "name": "(주)테크스타트업",
                                "industry": "IT/소프트웨어"
                              }
                            },
                            "type": "TEXT",
                            "mode": "PRACTICE",
                            "status": "COMPLETED",
                            "startTime": "2024-06-01T14:30:00",
                            "endTime": "2024-06-01T15:15:00",
                            "questionCount": 5,
                            "answeredQuestionCount": 5,
                            "createdAt": "2024-06-01T14:00:00"
                          }
                        ],
                        "totalElements": 10,
                        "totalPages": 2,
                        "size": 5,
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
    public ResponseEntity<CommonResponse<Page<InterviewSession>>> getMyInterviews(
            @Parameter(hidden = true) CurrentUser currentUser,
            @PageableDefault(size = 10)
            @Parameter(
                    description = "페이징 정보",
                    example = "page=0&size=10&sort=createdAt,desc"
            ) Pageable pageable
    ) {
        Page<InterviewSession> interviews = interviewService.getUserInterviews(currentUser.id(), pageable);
        return ResponseEntity.ok(CommonResponse.ok(interviews));
    }

    @GetMapping("/{interviewId}")
    @Operation(
            summary = "면접 상세 조회",
            description = "면접 ID로 면접 세션의 상세 정보를 조회합니다. 모든 질문과 답변, 평가 결과를 포함합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "면접 상세 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = InterviewSession.class),
                            examples = @ExampleObject(
                                    name = "면접 상세 응답 예시",
                                    value = """
                    {
                      "status": 200,
                      "data": {
                        "id": 1,
                        "user": { "id": 1, "userName": "홍길동", "nickname": "개발자홍길동" },
                        "resume": { "id": 1, "title": "백엔드 개발자 이력서" },
                        "position": { "id": 1, "name": "백엔드 개발자" },
                        "type": "TEXT",
                        "mode": "PRACTICE",
                        "status": "COMPLETED",
                        "startTime": "2024-06-01T14:30:00",
                        "endTime": "2024-06-01T15:15:00",
                        "shareUrl": "abc123-def456",
                        "questionCount": 5,
                        "currentQuestionIndex": 5,
                        "totalTimeSeconds": 2700,
                        "questions": [
                          {
                            "id": 1,
                            "content": "본인의 강점과 약점에 대해 설명해주세요.",
                            "type": "PERSONALITY",
                            "sequence": 1,
                            "difficultyLevel": 3,
                            "category": "인성면접",
                            "answer": {
                              "id": 1,
                              "content": "저의 강점은 문제 해결 능력입니다...",
                              "communicationScore": 8,
                              "technicalScore": 7,
                              "structureScore": 9
                            }
                          }
                        ]
                      }
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(responseCode = "404", description = "면접 세션을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<CommonResponse<InterviewSession>> getInterview(
            @Parameter(description = "조회할 면접 세션 ID", example = "1")
            @PathVariable Integer interviewId
    ) {
        InterviewSession interview = interviewService.getInterviewById(interviewId);
        return ResponseEntity.ok(CommonResponse.ok(interview));
    }

    @GetMapping("/shared/{shareUrl}")
    @Operation(
            summary = "공유된 면접 결과 조회",
            description = "공유 URL을 통해 면접 결과를 조회합니다. 면접자가 결과를 공개한 경우에만 접근 가능합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "공유 면접 결과 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = InterviewSession.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "공유 URL을 찾을 수 없거나 비공개 면접"),
            @ApiResponse(responseCode = "410", description = "만료된 공유 URL"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<CommonResponse<InterviewSession>> getSharedInterview(
            @Parameter(description = "면접 결과 공유 URL", example = "abc123-def456-ghi789")
            @PathVariable String shareUrl
    ) {
        InterviewSession interview = interviewService.getInterviewByShareUrl(shareUrl);
        return ResponseEntity.ok(CommonResponse.ok(interview));
    }

    @PostMapping
    @Operation(
            summary = "면접 세션 생성",
            description = """
                새로운 면접 세션을 생성합니다. AI를 사용하여 이력서와 포지션에 맞는 면접 질문을 자동 생성하거나,
                기존 질문들을 선택하여 면접을 구성할 수 있습니다.
                
                **AI 질문 생성 과정:**
                1. 이력서 내용과 포지션 정보 분석
                2. 맞춤형 면접 질문 생성 (기술, 인성, 경험 등)
                3. 난이도별 질문 구성
                4. 면접 세션에 질문 할당
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "면접 세션 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = InterviewSession.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "이력서 또는 포지션을 찾을 수 없음"),
            @ApiResponse(responseCode = "503", description = "AI 서비스 일시적 불가"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<CommonResponse<InterviewSession>> createInterview(
            @Parameter(hidden = true) CurrentUser currentUser,
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "면접 세션 생성 정보",
                    content = @Content(
                            schema = @Schema(implementation = CreateInterviewRequest.class),
                            examples = @ExampleObject(
                                    name = "면접 생성 요청 예시",
                                    value = """
                        {
                          "resumeId": 1,
                          "positionId": 1,
                          "title": "백엔드 개발자 포지션 면접 연습",
                          "description": "Spring Boot 관련 질문 위주로 연습하고 싶습니다.",
                          "type": "TEXT",
                          "mode": "PRACTICE",
                          "useAI": true,
                          "questionCount": 5,
                          "expectedDurationMinutes": 30,
                          "difficultyLevel": 3,
                          "categoryFilter": "기술면접",
                          "isPublic": false
                        }
                        """
                            )
                    )
            ) CreateInterviewRequest request
    ) {
        // 면접 세션 생성 로직 포함
        InterviewSession interview = interviewService.createInterview(currentUser.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.ok(interview));
    }

    @PostMapping("/{interviewId}/questions/generate")
    @Operation(
            summary = "추가 질문 생성",
            description = "기존 면접 세션에 AI를 사용하여 추가 질문을 생성합니다. 이력서와 포지션 정보를 바탕으로 맞춤형 질문을 제공합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "질문 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommonResponse.class),
                            examples = @ExampleObject(
                                    value = """
                    {
                      "status": 200,
                      "data": [
                        {
                          "id": 6,
                          "content": "REST API 설계 시 가장 중요하게 고려하는 원칙은 무엇인가요?",
                          "type": "TECHNICAL",
                          "sequence": 6,
                          "difficultyLevel": 4,
                          "category": "기술면접"
                        }
                      ]
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "404", description = "면접 세션을 찾을 수 없음"),
            @ApiResponse(responseCode = "503", description = "AI 서비스 일시적 불가"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<CommonResponse<List<Question>>> generateQuestions(
            @Parameter(description = "질문을 추가할 면접 세션 ID", example = "1")
            @PathVariable Integer interviewId,
            @Valid @RequestBody GenerateQuestionsRequest request
    ) {
        List<Question> questions = mlIntegrationService.generateInterviewQuestions(
                request.getResumeContent(),
                request.getPosition(),
                request.getQuestionCount()
        );

        for (Question question : questions) {
            interviewService.addQuestionToInterview(interviewId, question);
        }

        return ResponseEntity.ok(CommonResponse.ok(questions));
    }

    @PostMapping("/{interviewId}/start")
    @Operation(
            summary = "면접 시작",
            description = "면접 세션을 시작 상태로 변경합니다. 면접 시작 시간이 기록되고 상태가 IN_PROGRESS로 변경됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "면접 시작 성공"),
            @ApiResponse(responseCode = "400", description = "이미 시작된 면접이거나 시작할 수 없는 상태"),
            @ApiResponse(responseCode = "404", description = "면접 세션을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Void> startInterview(
            @Parameter(description = "시작할 면접 세션 ID", example = "1")
            @PathVariable Integer interviewId
    ) {
        interviewService.startInterview(interviewId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{interviewId}/complete")
    @Operation(
            summary = "면접 완료",
            description = "면접 세션을 완료 상태로 변경합니다. 면접 종료 시간이 기록되고 상태가 COMPLETED로 변경됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "면접 완료 성공"),
            @ApiResponse(responseCode = "400", description = "완료할 수 없는 면접 상태"),
            @ApiResponse(responseCode = "404", description = "면접 세션을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Void> completeInterview(
            @Parameter(description = "완료할 면접 세션 ID", example = "1")
            @PathVariable Integer interviewId
    ) {
        interviewService.completeInterview(interviewId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{interviewId}/questions")
    @Operation(
            summary = "면접 질문 목록 조회",
            description = "특정 면접 세션의 모든 질문을 순서대로 조회합니다. 답변 정보도 함께 포함됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "질문 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommonResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "면접 세션을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<CommonResponse<List<Question>>> getInterviewQuestions(
            @Parameter(description = "질문을 조회할 면접 세션 ID", example = "1")
            @PathVariable Integer interviewId
    ) {
        List<Question> questions = interviewService.getInterviewQuestions(interviewId);
        return ResponseEntity.ok(CommonResponse.ok(questions));
    }

    @GetMapping("/{interviewId}/next-question")
    @Operation(
            summary = "다음 질문 조회",
            description = "면접 진행 중 다음 질문을 조회합니다. 현재 진행 상황에 따라 순서대로 질문을 제공합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "다음 질문 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Question.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "면접 세션을 찾을 수 없음"),
            @ApiResponse(responseCode = "410", description = "더 이상 질문이 없음 (면접 완료)"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<CommonResponse<Question>> getNextQuestion(
            @Parameter(description = "면접 세션 ID", example = "1")
            @PathVariable Integer interviewId
    ) {
        Question question = interviewService.getNextQuestion(interviewId);
        return ResponseEntity.ok(CommonResponse.ok(question));
    }

    @PostMapping("/questions/{questionId}/answer")
    @Operation(
            summary = "텍스트 답변 제출",
            description = """
                면접 질문에 대한 텍스트 답변을 제출합니다. AI를 사용한 자동 평가가 활성화된 경우,
                답변의 관련성, 구체성, 실무성, 유효성을 종합적으로 평가하여 피드백을 제공합니다.
                
                **AI 평가 항목:**
                - **관련성**: 질문과 답변의 연관성
                - **구체성**: 구체적인 사례와 경험 포함 여부
                - **실무성**: 실무에 적용 가능한 내용인지
                - **유효성**: 논리적이고 타당한 답변인지
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "답변 제출 및 평가 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AnswerWithEvaluation.class),
                            examples = @ExampleObject(
                                    value = """
                    {
                      "status": 201,
                      "data": {
                        "answer": {
                          "id": 1,
                          "content": "저의 강점은 문제 해결 능력입니다...",
                          "feedback": "구체적인 사례를 잘 제시했습니다.",
                          "communicationScore": 8,
                          "technicalScore": 7,
                          "structureScore": 9
                        },
                        "evaluation": {
                          "id": 1,
                          "relevance": 9,
                          "specificity": 8,
                          "practicality": 7,
                          "validity": 8,
                          "totalScore": 32,
                          "feedback": "전반적으로 우수한 답변입니다."
                        }
                      }
                    }
                    """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 답변 데이터"),
            @ApiResponse(responseCode = "404", description = "질문을 찾을 수 없음"),
            @ApiResponse(responseCode = "503", description = "AI 평가 서비스 일시적 불가"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<CommonResponse<AnswerWithEvaluation>> submitTextAnswer(
            @Parameter(description = "답변할 질문 ID", example = "1")
            @PathVariable Integer questionId,
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "제출할 답변 정보",
                    content = @Content(
                            schema = @Schema(implementation = SubmitAnswerRequest.class),
                            examples = @ExampleObject(
                                    value = """
                        {
                          "content": "저의 가장 큰 강점은 문제 해결 능력입니다. 이전 프로젝트에서 성능 이슈가 발생했을 때, 프로파일링 도구를 활용하여 병목 지점을 찾아내고 쿼리 최적화를 통해 응답속도를 50% 개선한 경험이 있습니다.",
                          "useAIEvaluation": true,
                          "resumeContent": "3년차 백엔드 개발자, Spring Boot 전문가...",
                          "coverLetter": "항상 새로운 기술을 학습하며 성장하는 개발자입니다..."
                        }
                        """
                            )
                    )
            ) SubmitAnswerRequest request
    ) {
        Answer answer = interviewService.submitTextAnswer(questionId, request.getContent());

        AnswerEvaluation evaluation = null;
        if (request.isUseAIEvaluation()) {
            Question question = interviewService.getQuestionById(questionId);
            evaluation = mlIntegrationService.evaluateAnswer(
                    question.getContent(),
                    request.getContent(),
                    request.getResumeContent(),
                    request.getCoverLetter()
            );
            interviewService.saveAnswerEvaluation(answer.getId(), evaluation);
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.ok(new AnswerWithEvaluation(answer, evaluation)));
    }

    @PostMapping(value = "/questions/{questionId}/answer/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "음성 답변 제출",
            description = """
                면접 질문에 대한 음성 답변을 제출합니다.
                업로드된 음성 파일은 자동으로 텍스트로 변환되어 처리되며, AI 평가도 함께 진행됩니다.
                
                **지원 파일 형식:** WAV, MP3, M4A
                **최대 파일 크기:** 10MB
                **권장 녹음 시간:** 1-5분
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "음성 답변 제출 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Answer.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "지원하지 않는 파일 형식"),
            @ApiResponse(responseCode = "404", description = "질문을 찾을 수 없음"),
            @ApiResponse(responseCode = "413", description = "파일 크기 초과"),
            @ApiResponse(responseCode = "503", description = "음성 인식 서비스 일시적 불가"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<CommonResponse<Answer>> submitAudioAnswer(
            @Parameter(description = "답변할 질문 ID", example = "1")
            @PathVariable Integer questionId,
            @Parameter(
                    description = "음성 답변 파일 (WAV, MP3, M4A)",
                    content = @Content(mediaType = "multipart/form-data")
            )
            @RequestParam("file") MultipartFile audioFile
    ) {
        Answer answer = interviewService.submitAudioAnswer(questionId, audioFile);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.ok(answer));
    }

    @GetMapping("/questions/{questionId}/audio")
    @Operation(
            summary = "질문 음성 파일 조회",
            description = "면접 질문의 TTS(Text-to-Speech) 음성 파일 URL을 조회합니다. 음성 면접 모드에서 사용됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "음성 파일 URL 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(type = "string", example = "https://storage.example.com/questions/audio/question_1.mp3")
                    )
            ),
            @ApiResponse(responseCode = "404", description = "질문을 찾을 수 없음"),
            @ApiResponse(responseCode = "503", description = "TTS 서비스 일시적 불가"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<CommonResponse<String>> getQuestionAudio(
            @Parameter(description = "음성 파일을 조회할 질문 ID", example = "1")
            @PathVariable Integer questionId
    ) {
        String audioUrl = interviewService.getQuestionAudio(questionId);
        return ResponseEntity.ok(CommonResponse.ok(audioUrl));
    }

    @PostMapping("/{interviewId}/simulate")
    @Operation(
            summary = "면접 시뮬레이션",
            description = """
                전체 면접 과정을 시뮬레이션합니다. 이력서, 자기소개서, 채용공고를 분석하여
                맞춤형 질문을 생성하고, 제출된 답변에 대한 종합적인 평가를 제공합니다.
                
                **시뮬레이션 과정:**
                1. 채용공고와 이력서 매칭도 분석
                2. 맞춤형 면접 질문 생성
                3. 답변 평가 및 개선점 제안
                4. 종합 면접 결과 리포트 생성
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "면접 시뮬레이션 완료",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = InterviewSimulationResult.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 시뮬레이션 요청 데이터"),
            @ApiResponse(responseCode = "404", description = "면접 세션을 찾을 수 없음"),
            @ApiResponse(responseCode = "503", description = "AI 시뮬레이션 서비스 일시적 불가"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<CommonResponse<InterviewSimulationResult>> simulateInterview(
            @Parameter(description = "시뮬레이션할 면접 세션 ID", example = "1")
            @PathVariable Integer interviewId,
            @Valid @RequestBody SimulateInterviewRequest request
    ) {
        InterviewSimulationResult result = mlIntegrationService.simulateInterview(
                request.getResumeContent(),
                request.getCoverLetter(),
                request.getJobDescription(),
                request.getUserAnswer(),
                request.getNumQuestions()
        );

        interviewService.saveSimulationResult(interviewId, result);
        return ResponseEntity.ok(CommonResponse.ok(result));
    }

    @PostMapping("/{interviewId}/share")
    @Operation(
            summary = "면접 결과 공유 URL 생성",
            description = "면접 결과를 다른 사람과 공유할 수 있는 URL을 생성합니다. 생성된 URL을 통해 면접 결과를 조회할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "공유 URL 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(type = "string", example = "abc123-def456-ghi789")
                    )
            ),
            @ApiResponse(responseCode = "400", description = "공유할 수 없는 면접 상태"),
            @ApiResponse(responseCode = "404", description = "면접 세션을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<CommonResponse<String>> generateShareUrl(
            @Parameter(description = "공유할 면접 세션 ID", example = "1")
            @PathVariable Integer interviewId
    ) {
        String shareUrl = interviewService.generateShareUrl(interviewId);
        return ResponseEntity.ok(CommonResponse.ok(shareUrl));
    }

    @PostMapping("/{interviewId}/time")
    @Operation(
            summary = "면접 소요 시간 업데이트",
            description = "면접에 소요된 총 시간을 업데이트합니다. 클라이언트에서 측정된 시간을 서버에 저장합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "시간 업데이트 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 시간 데이터"),
            @ApiResponse(responseCode = "404", description = "면접 세션을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Void> updateInterviewTime(
            @Parameter(description = "시간을 업데이트할 면접 세션 ID", example = "1")
            @PathVariable Integer interviewId,
            @Valid @RequestBody UpdateTimeRequest request
    ) {
        interviewService.updateInterviewTime(interviewId, request.getTimeInSeconds());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/questions/search")
    @Operation(
            summary = "질문 검색",
            description = "카테고리, 난이도, 타입, 키워드로 면접 질문을 검색합니다. 면접 준비나 질문 선택 시 활용할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "질문 검색 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommonResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 검색 파라미터"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<CommonResponse<Page<Question>>> searchQuestions(
            @Parameter(description = "질문 카테고리", example = "기술면접", schema = @Schema(allowableValues = {"기술면접", "인성면접", "경험면접", "상황면접"}))
            @RequestParam(required = false) String category,
            @Parameter(description = "난이도 레벨 (1-5)", example = "3", schema = @Schema(minimum = "1", maximum = "5"))
            @RequestParam(required = false) Integer difficultyLevel,
            @Parameter(description = "질문 타입", example = "TECHNICAL", schema = @Schema(allowableValues = {"PERSONALITY", "TECHNICAL", "PROJECT", "SITUATION"}))
            @RequestParam(required = false) QuestionType type,
            @Parameter(description = "검색 키워드", example = "Spring Boot")
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<Question> questions = interviewService.searchQuestions(
                category,
                difficultyLevel,
                type,
                keyword,
                pageable
        );
        return ResponseEntity.ok(CommonResponse.ok(questions));
    }

    @PostMapping("/questions/{questionId}/favorite")
    @Operation(
            summary = "질문 즐겨찾기 토글",
            description = "면접 질문을 즐겨찾기에 추가하거나 제거합니다. 자주 연습하고 싶은 질문을 관리할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "즐겨찾기 상태 변경 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "질문을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Void> toggleFavoriteQuestion(
            @Parameter(hidden = true) CurrentUser currentUser,
            @Parameter(description = "즐겨찾기 토글할 질문 ID", example = "1")
            @PathVariable Integer questionId
    ) {
        interviewService.toggleFavoriteQuestion(currentUser.id(), questionId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/questions/favorites")
    @Operation(
            summary = "즐겨찾기 질문 목록 조회",
            description = "현재 사용자가 즐겨찾기로 등록한 면접 질문 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "즐겨찾기 질문 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommonResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<CommonResponse<List<Question>>> getFavoriteQuestions(
            @Parameter(hidden = true) CurrentUser currentUser
    ) {
        List<Question> questions = interviewService.getFavoriteQuestions(currentUser.id());
        return ResponseEntity.ok(CommonResponse.ok(questions));
    }
}