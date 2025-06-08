package dev.swote.interv.controller;

import dev.swote.interv.config.CommonResponse;
import dev.swote.interv.domain.interview.DTO.*;
import dev.swote.interv.domain.interview.entity.*;
import dev.swote.interv.domain.resume.entity.Resume;
import dev.swote.interv.domain.position.entity.Position;
import dev.swote.interv.interceptor.CurrentUser;
import dev.swote.interv.service.ai.MLIntegrationService;
import dev.swote.interv.service.interview.InterviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;
    private final MLIntegrationService mlIntegrationService;

    @GetMapping
    public ResponseEntity<CommonResponse<Page<InterviewSession>>> getMyInterviews(
            CurrentUser currentUser,
            Pageable pageable
    ) {
        Page<InterviewSession> interviews = interviewService.getUserInterviews(currentUser.id(), pageable);
        return ResponseEntity.ok(CommonResponse.ok(interviews));
    }

    @GetMapping("/{interviewId}")
    public ResponseEntity<CommonResponse<InterviewSession>> getInterview(
            @PathVariable Integer interviewId
    ) {
        InterviewSession interview = interviewService.getInterviewById(interviewId);
        return ResponseEntity.ok(CommonResponse.ok(interview));
    }

    @GetMapping("/shared/{shareUrl}")
    public ResponseEntity<CommonResponse<InterviewSession>> getSharedInterview(
            @PathVariable String shareUrl
    ) {
        InterviewSession interview = interviewService.getInterviewByShareUrl(shareUrl);
        return ResponseEntity.ok(CommonResponse.ok(interview));
    }

    /**
     * 면접 세션 생성 - ML API를 사용한 질문 자동 생성
     */
    @PostMapping
    public ResponseEntity<CommonResponse<InterviewSession>> createInterview(
            CurrentUser currentUser,
            @RequestBody CreateInterviewRequest request
    ) {
        // 1. 기본 면접 세션 생성
        InterviewSession interview = interviewService.createInterview(currentUser.id(), request);

        // 2. 이력서와 포지션 정보 가져오기
        Resume resume = interviewService.getResumeById(request.getResumeId());
        Position position = interviewService.getPositionById(request.getPositionId());

        // 3. ML API를 통한 질문 생성
        if (request.isUseAI()) {
            log.info("AI를 사용하여 면접 질문 생성 시작 - 세션 ID: {}", interview.getId());

            List<Question> aiQuestions = mlIntegrationService.generateInterviewQuestions(
                    resume.getContent(),
                    position.getTitle(),
                    request.getQuestionCount()
            );

            // 4. 생성된 질문들을 면접 세션에 추가
            for (Question question : aiQuestions) {
                interviewService.addQuestionToInterview(interview.getId(), question);
            }

            log.info("AI 질문 생성 완료 - {} 개 질문 추가됨", aiQuestions.size());
        }

        // 5. 관계 설정
        interview.setUser(interviewService.getUserById(currentUser.id()));
        interview.setResume(resume);
        interview.setPosition(position);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.ok(interview));
    }

    /**
     * 면접 질문 생성 (기존 면접에 질문 추가)
     */
    @PostMapping("/{interviewId}/questions/generate")
    public ResponseEntity<CommonResponse<List<Question>>> generateQuestions(
            @PathVariable Integer interviewId,
            @RequestBody GenerateQuestionsRequest request
    ) {
        // 면접 세션 정보 가져오기
        InterviewSession interview = interviewService.getInterviewById(interviewId);

        // ML API를 통한 질문 생성
        List<Question> questions = mlIntegrationService.generateInterviewQuestions(
                request.getResumeContent(),
                request.getPosition(),
                request.getQuestionCount()
        );

        // 생성된 질문들을 면접에 추가
        for (Question question : questions) {
            interviewService.addQuestionToInterview(interviewId, question);
        }

        log.info("면접 {}에 {} 개의 질문이 추가되었습니다", interviewId, questions.size());

        return ResponseEntity.ok(CommonResponse.ok(questions));
    }

    @PostMapping("/{interviewId}/start")
    public ResponseEntity<Void> startInterview(
            @PathVariable Integer interviewId
    ) {
        interviewService.startInterview(interviewId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{interviewId}/complete")
    public ResponseEntity<Void> completeInterview(
            @PathVariable Integer interviewId
    ) {
        interviewService.completeInterview(interviewId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{interviewId}/questions")
    public ResponseEntity<CommonResponse<List<Question>>> getInterviewQuestions(
            @PathVariable Integer interviewId
    ) {
        List<Question> questions = interviewService.getInterviewQuestions(interviewId);
        return ResponseEntity.ok(CommonResponse.ok(questions));
    }

    @GetMapping("/{interviewId}/next-question")
    public ResponseEntity<CommonResponse<Question>> getNextQuestion(
            @PathVariable Integer interviewId
    ) {
        Question question = interviewService.getNextQuestion(interviewId);
        return ResponseEntity.ok(CommonResponse.ok(question));
    }

    /**
     * 답변 제출 - ML API를 사용한 자동 평가
     */
    @PostMapping("/questions/{questionId}/answer")
    public ResponseEntity<CommonResponse<AnswerWithEvaluation>> submitTextAnswer(
            @PathVariable Integer questionId,
            @RequestBody SubmitAnswerRequest request
    ) {
        // 1. 답변 저장
        Answer answer = interviewService.submitTextAnswer(questionId, request.getContent());

        // 2. ML API를 통한 답변 평가
        AnswerEvaluation evaluation = null;
        if (request.isUseAIEvaluation()) {
            Question question = interviewService.getQuestionById(questionId);

            evaluation = mlIntegrationService.evaluateAnswer(
                    question.getContent(),
                    request.getContent(),
                    request.getResumeContent(),
                    request.getCoverLetter()
            );

            // 평가 결과 저장
            interviewService.saveAnswerEvaluation(answer.getId(), evaluation);

            log.info("답변 평가 완료 - 질문 ID: {}, 총점: {}", questionId, evaluation.getTotalScore());
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.ok(new AnswerWithEvaluation(answer, evaluation)));
    }

    @PostMapping(value = "/questions/{questionId}/answer/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<Answer>> submitAudioAnswer(
            @PathVariable Integer questionId,
            @RequestParam("file") MultipartFile audioFile
    ) {
        Answer answer = interviewService.submitAudioAnswer(questionId, audioFile);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.ok(answer));
    }

    @GetMapping("/questions/{questionId}/audio")
    public ResponseEntity<CommonResponse<String>> getQuestionAudio(
            @PathVariable Integer questionId
    ) {
        String audioUrl = interviewService.getQuestionAudio(questionId);
        return ResponseEntity.ok(CommonResponse.ok(audioUrl));
    }

    /**
     * 면접 시뮬레이션 - 전체 면접 과정 시뮬레이션
     */
    @PostMapping("/{interviewId}/simulate")
    public ResponseEntity<CommonResponse<InterviewSimulationResult>> simulateInterview(
            @PathVariable Integer interviewId,
            @RequestBody SimulateInterviewRequest request
    ) {
        InterviewSession interview = interviewService.getInterviewById(interviewId);

        // ML API를 통한 면접 시뮬레이션
        InterviewSimulationResult result = mlIntegrationService.simulateInterview(
                request.getResumeContent(),
                request.getCoverLetter(),
                request.getJobDescription(),
                request.getUserAnswer(),
                request.getNumQuestions()
        );

        // 시뮬레이션 결과 저장
        interviewService.saveSimulationResult(interviewId, result);

        return ResponseEntity.ok(CommonResponse.ok(result));
    }

    @PostMapping("/{interviewId}/share")
    public ResponseEntity<CommonResponse<String>> generateShareUrl(
            @PathVariable Integer interviewId
    ) {
        String shareUrl = interviewService.generateShareUrl(interviewId);
        return ResponseEntity.ok(CommonResponse.ok(shareUrl));
    }

    @PostMapping("/{interviewId}/time")
    public ResponseEntity<Void> updateInterviewTime(
            @PathVariable Integer interviewId,
            @RequestBody UpdateTimeRequest request
    ) {
        interviewService.updateInterviewTime(interviewId, request.getTimeInSeconds());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/questions/search")
    public ResponseEntity<CommonResponse<Page<Question>>> searchQuestions(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer difficultyLevel,
            @RequestParam(required = false) QuestionType type,
            @RequestParam(required = false) String keyword,
            Pageable pageable
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
    public ResponseEntity<Void> toggleFavoriteQuestion(
            CurrentUser currentUser,
            @PathVariable Integer questionId
    ) {
        interviewService.toggleFavoriteQuestion(currentUser.id(), questionId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/questions/favorites")
    public ResponseEntity<CommonResponse<List<Question>>> getFavoriteQuestions(
            CurrentUser currentUser
    ) {
        List<Question> questions = interviewService.getFavoriteQuestions(currentUser.id());
        return ResponseEntity.ok(CommonResponse.ok(questions));
    }

}