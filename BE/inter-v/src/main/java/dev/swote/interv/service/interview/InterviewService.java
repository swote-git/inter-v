package dev.swote.interv.service.interview;

import dev.swote.interv.domain.interview.entity.*;
import dev.swote.interv.domain.interview.repository.AnswerRepository;
import dev.swote.interv.domain.interview.repository.InterviewSessionRepository;
import dev.swote.interv.domain.interview.repository.QuestionRepository;
import dev.swote.interv.domain.position.entity.Position;
import dev.swote.interv.domain.position.repository.PositionRepository;
import dev.swote.interv.domain.resume.entity.Resume;
import dev.swote.interv.domain.resume.repository.ResumeRepository;
import dev.swote.interv.domain.user.entity.User;
import dev.swote.interv.domain.user.repository.UserRepository;
import dev.swote.interv.service.ai.LlmService;
import dev.swote.interv.service.audio.AudioStorageService;
import dev.swote.interv.service.audio.SpeechToTextService;
import dev.swote.interv.service.audio.TextToSpeechService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewSessionRepository interviewSessionRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final PositionRepository positionRepository;
    private final LlmService llmService;
    private final AudioStorageService audioStorageService;
    private final TextToSpeechService textToSpeechService;
    private final SpeechToTextService speechToTextService;

    @Transactional(readOnly = true)
    public Page<InterviewSession> getUserInterviews(Integer userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return interviewSessionRepository.findByUser(user, pageable);
    }

    @Transactional(readOnly = true)
    public InterviewSession getInterviewById(Integer interviewId) {
        return interviewSessionRepository.findById(interviewId)
                .orElseThrow(() -> new RuntimeException("Interview session not found"));
    }

    @Transactional(readOnly = true)
    public InterviewSession getInterviewByShareUrl(String shareUrl) {
        return interviewSessionRepository.findByShareUrl(shareUrl)
                .orElseThrow(() -> new RuntimeException("Interview session not found"));
    }

    @Transactional
    public InterviewSession createInterview(Integer userId, CreateInterviewRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Resume resume = null;
        if (request.getResumeId() != null) {
            resume = resumeRepository.findById(request.getResumeId())
                    .orElseThrow(() -> new RuntimeException("Resume not found"));
        }

        Position position = null;
        if (request.getPositionId() != null) {
            position = positionRepository.findById(request.getPositionId())
                    .orElseThrow(() -> new RuntimeException("Position not found"));
        }

        String shareUrl = UUID.randomUUID().toString();

        InterviewSession interviewSession = InterviewSession.builder()
                .user(user)
                .resume(resume)
                .position(position)
                .type(request.getType())
                .mode(request.getMode())
                .status(InterviewStatus.SCHEDULED)
                .startTime(LocalDateTime.now())
                .shareUrl(shareUrl)
                .questionCount(request.getQuestionCount())
                .currentQuestionIndex(0)
                .totalTimeSeconds(0)
                .build();

        interviewSession = interviewSessionRepository.save(interviewSession);

        // Generate or select questions based on mode and type
        List<Question> questions;

        if (request.getQuestionIds() != null && !request.getQuestionIds().isEmpty()) {
            // If specific questions were selected
            questions = request.getQuestionIds().stream()
                    .map(id -> questionRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("Question not found: " + id)))
                    .collect(Collectors.toList());
        } else if (InterviewMode.PRACTICE.equals(request.getMode())) {
            // For practice mode, use predefined questions from the database
            questions = questionRepository.findRandomQuestions(
                    request.getType().toString(),
                    request.getQuestionCount());
        } else {
            // For real mode, generate questions based on resume and position
            questions = llmService.generateInterviewQuestions(resume, position, request.getQuestionCount());
        }

        // Set sequence numbers and save questions
        int sequence = 1;
        for (Question question : questions) {
            question.setInterviewSession(interviewSession);
            question.setSequence(sequence++);
            questionRepository.save(question);
        }

        return interviewSession;
    }

    @Transactional
    public void startInterview(Integer interviewId) {
        InterviewSession interviewSession = interviewSessionRepository.findById(interviewId)
                .orElseThrow(() -> new RuntimeException("Interview session not found"));

        interviewSession.setStatus(InterviewStatus.IN_PROGRESS);
        interviewSession.setStartTime(LocalDateTime.now());
        interviewSessionRepository.save(interviewSession);
    }

    @Transactional
    public void completeInterview(Integer interviewId) {
        InterviewSession interviewSession = interviewSessionRepository.findById(interviewId)
                .orElseThrow(() -> new RuntimeException("Interview session not found"));

        interviewSession.setStatus(InterviewStatus.COMPLETED);
        interviewSession.setEndTime(LocalDateTime.now());
        interviewSessionRepository.save(interviewSession);
    }

    @Transactional(readOnly = true)
    public List<Question> getInterviewQuestions(Integer interviewId) {
        InterviewSession interviewSession = interviewSessionRepository.findById(interviewId)
                .orElseThrow(() -> new RuntimeException("Interview session not found"));

        return questionRepository.findByInterviewSessionOrderBySequenceAsc(interviewSession);
    }

    @Transactional
    public Answer submitTextAnswer(Integer questionId, String answerContent) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        Answer answer = llmService.provideFeedback(question, answerContent);
        return answerRepository.save(answer);
    }

    @Transactional
    public Answer submitAudioAnswer(Integer questionId, MultipartFile audioFile) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        String audioFilePath = audioStorageService.storeAudioFile(audioFile);

        // Start transcription job
        String jobName = speechToTextService.startTranscriptionJob(audioFilePath);

        // In a real application, you'd use a job queue to poll for the result
        String transcriptionUrl = speechToTextService.getTranscriptionResult(jobName);
        String transcribedText = speechToTextService.extractTranscribedText(transcriptionUrl);

        Answer answer = llmService.provideFeedback(question, transcribedText);
        answer.setAudioFilePath(audioFilePath);

        return answerRepository.save(answer);
    }

    @Transactional
    public String getQuestionAudio(Integer questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        return textToSpeechService.convertTextToSpeech(question.getContent());
    }

    @Transactional
    public String generateShareUrl(Integer interviewId) {
        InterviewSession interviewSession = interviewSessionRepository.findById(interviewId)
                .orElseThrow(() -> new RuntimeException("Interview session not found"));

        if (interviewSession.getShareUrl() == null) {
            interviewSession.setShareUrl(UUID.randomUUID().toString());
            interviewSessionRepository.save(interviewSession);
        }

        return interviewSession.getShareUrl();
    }

    @Transactional
    public Question getNextQuestion(Integer interviewId) {
        InterviewSession session = interviewSessionRepository.findById(interviewId)
                .orElseThrow(() -> new RuntimeException("Interview session not found"));

        if (session.getCurrentQuestionIndex() >= session.getQuestionCount()) {
            throw new RuntimeException("No more questions available");
        }

        List<Question> questions = questionRepository.findByInterviewSessionOrderBySequenceAsc(session);
        Question nextQuestion = questions.get(session.getCurrentQuestionIndex());

        // Update the current question index
        session.setCurrentQuestionIndex(session.getCurrentQuestionIndex() + 1);
        interviewSessionRepository.save(session);

        return nextQuestion;
    }

    @Transactional
    public void updateInterviewTime(Integer interviewId, Integer timeInSeconds) {
        InterviewSession session = interviewSessionRepository.findById(interviewId)
                .orElseThrow(() -> new RuntimeException("Interview session not found"));

        session.setTotalTimeSeconds(timeInSeconds);
        interviewSessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public Page<Question> searchQuestions(
            String category,
            Integer difficultyLevel,
            QuestionType type,
            String keyword,
            Pageable pageable) {

        return questionRepository.findQuestionsByFilters(
                category,
                difficultyLevel,
                type,
                keyword,
                pageable
        );
    }

    @Transactional
    public void toggleFavoriteQuestion(Integer userId, Integer questionId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        if (user.getFavoritedQuestions().contains(question)) {
            user.getFavoritedQuestions().remove(question);
        } else {
            user.getFavoritedQuestions().add(question);
        }

        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<Question> getFavoriteQuestions(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return new ArrayList<>(user.getFavoritedQuestions());
    }

    public static class CreateInterviewRequest {
        private Integer resumeId;
        private Integer positionId;
        private InterviewType type;
        private InterviewMode mode;
        private Integer questionCount;
        private List<Integer> questionIds;  // For selecting specific questions

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

        public InterviewMode getMode() {
            return mode;
        }

        public void setMode(InterviewMode mode) {
            this.mode = mode;
        }

        public Integer getQuestionCount() {
            return questionCount;
        }

        public void setQuestionCount(Integer questionCount) {
            this.questionCount = questionCount;
        }

        public List<Integer> getQuestionIds() {
            return questionIds;
        }

        public void setQuestionIds(List<Integer> questionIds) {
            this.questionIds = questionIds;
        }
    }
}