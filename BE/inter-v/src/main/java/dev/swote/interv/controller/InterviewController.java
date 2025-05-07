package dev.swote.interv.controller;

import dev.swote.interv.config.CommonResponse;
import dev.swote.interv.domain.interview.entity.*;
import dev.swote.interv.interceptor.CurrentUser;
import dev.swote.interv.service.interview.InterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

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

    @PostMapping
    public ResponseEntity<CommonResponse<InterviewSession>> createInterview(
            CurrentUser currentUser,
            @RequestBody InterviewService.CreateInterviewRequest request
    ) {
        InterviewSession interview = interviewService.createInterview(
                currentUser.id(),
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.ok(interview));
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

    @PostMapping("/questions/{questionId}/answer")
    public ResponseEntity<CommonResponse<Answer>> submitTextAnswer(
            @PathVariable Integer questionId,
            @RequestBody SubmitAnswerRequest request
    ) {
        Answer answer = interviewService.submitTextAnswer(questionId, request.getContent());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.ok(answer));
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

    public static class SubmitAnswerRequest {
        private String content;

        // Getters and setters
        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    public static class UpdateTimeRequest {
        private Integer timeInSeconds;

        public Integer getTimeInSeconds() {
            return timeInSeconds;
        }

        public void setTimeInSeconds(Integer timeInSeconds) {
            this.timeInSeconds = timeInSeconds;
        }
    }
}