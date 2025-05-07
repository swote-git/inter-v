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
import java.util.List;
import java.util.UUID;

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
    public InterviewSession createInterview(Integer userId, Integer resumeId, Integer positionId, InterviewType type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new RuntimeException("Resume not found"));

        Position position = positionRepository.findById(positionId)
                .orElseThrow(() -> new RuntimeException("Position not found"));

        String shareUrl = UUID.randomUUID().toString();

        InterviewSession interviewSession = InterviewSession.builder()
                .user(user)
                .resume(resume)
                .position(position)
                .type(type)
                .status(InterviewStatus.SCHEDULED)
                .startTime(LocalDateTime.now())
                .shareUrl(shareUrl)
                .build();

        interviewSession = interviewSessionRepository.save(interviewSession);

        // Generate questions
        List<Question> questions = llmService.generateInterviewQuestions(resume, position, 5);

        for (Question question : questions) {
            question.setInterviewSession(interviewSession);
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

    @Transactional
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
        // For simplicity, we'll assume it's immediate
        String transcriptionUrl = speechToTextService.getTranscriptionResult(jobName);

        // For simplicity, let's assume we can extract the text directly
        // In reality, you'd need to fetch and parse the JSON from the transcription URL
        String transcribedText = "Example transcribed text"; // This would be the actual text

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
}