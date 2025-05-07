package dev.swote.interv.controller;

import dev.swote.interv.config.CommonResponse;
import dev.swote.interv.domain.interview.entity.Answer;
import dev.swote.interv.domain.interview.entity.InterviewSession;
import dev.swote.interv.domain.interview.entity.InterviewType;
import dev.swote.interv.domain.interview.entity.Question;
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
            @RequestBody CreateInterviewRequest request
    ) {
        InterviewSession interview = interviewService.createInterview(
                currentUser.id(),
                request.getResumeId(),
                request.getPositionId(),
                request.getType()
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

    public static class CreateInterviewRequest {
        private Integer resumeId;
        private Integer positionId;
        private InterviewType type;

        // Getters and setters
        public Integer getResumeId() {
            return resumeId;
        }

        public void setResumeId(Integer resumeId) {
            this.resumeId = resumeId;
        }

        public Integer getPositionId() {
            return positionId;
        }

        public void setPositionId(Integer positionId) {
            this.positionId = positionId;
        }

        public InterviewType getType() {
            return type;
        }

        public void setType(InterviewType type) {
            this.type = type;
        }
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
}