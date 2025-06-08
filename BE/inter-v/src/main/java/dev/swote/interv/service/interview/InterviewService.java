package dev.swote.interv.service.interview;

import dev.swote.interv.domain.interview.DTO.*;
import dev.swote.interv.domain.interview.repository.*;
import dev.swote.interv.exception.*;
import lombok.extern.slf4j.Slf4j;
import dev.swote.interv.domain.interview.entity.*;
import dev.swote.interv.domain.position.entity.Position;
import dev.swote.interv.domain.position.repository.PositionRepository;
import dev.swote.interv.domain.resume.entity.Resume;
import dev.swote.interv.domain.resume.repository.ResumeRepository;
import dev.swote.interv.domain.user.entity.User;
import dev.swote.interv.domain.user.repository.UserRepository;
import dev.swote.interv.service.ai.LlmService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewSessionRepository interviewSessionRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final PositionRepository positionRepository;
    private final AnswerEvaluationRepository answerEvaluationRepository;
    private final InterviewSimulationRepository interviewSimulationRepository;

    private final LlmService llmService;

    @Transactional(readOnly = true)
    public Page<InterviewSession> getUserInterviews(Integer userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return interviewSessionRepository.findByUser(user, pageable);
    }

    @Transactional(readOnly = true)
    public InterviewSession getInterviewById(Integer interviewId) {
        return interviewSessionRepository.findById(interviewId)
                .orElseThrow(() -> new InterviewSessionNotFoundException(interviewId));
    }

    @Transactional(readOnly = true)
    public InterviewSession getInterviewByShareUrl(String shareUrl) {
        return interviewSessionRepository.findByShareUrl(shareUrl)
                .orElseThrow(() -> new InterviewSessionNotFoundException(shareUrl));
    }

    @Transactional
    public InterviewSession createInterview(Integer userId, CreateInterviewRequest request) {
        log.info("면접 생성 시작 - 사용자: {}, 모드: {}, 질문 수: {}", userId, request.getMode(), request.getQuestionCount());

        // 유저 처리 (DB 없을 경우 더미 유저 생성)
        User user = getOrCreateUser(userId);

        // 이력서 처리
        Resume resume = getOrCreateResume(request.getResumeId());

        // 포지션 처리
        Position position = getOrCreatePosition(request.getPositionId());

        // 세션 생성
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

        // 질문 생성 또는 조회
        List<Question> questions = generateOrRetrieveQuestions(request, resume, position, interviewSession);

        // 질문 순서 및 세션 설정
        assignQuestionsToSession(questions, interviewSession);

        log.info("면접 생성 완료 - ID: {}, 질문 수: {}", interviewSession.getId(), questions.size());
        return interviewSession;
    }

    /**
     * 사용자 조회 또는 생성
     */
    private User getOrCreateUser(Integer userId) {
        return userRepository.findById(userId).orElseGet(() -> {
            log.warn("사용자를 찾을 수 없음 (ID: {}). 더미 사용자 생성", userId);
            User dummyUser = new User();
            dummyUser.setName("테스트 사용자");
            dummyUser.setEmail("test@example.com");
            return userRepository.save(dummyUser);
        });
    }

    /**
     * 이력서 조회 또는 생성
     */
    private Resume getOrCreateResume(Integer resumeId) {
        if (resumeId == null) {
            log.info("이력서 ID가 제공되지 않음. 기본 이력서 생성");
            Resume defaultResume = new Resume();
            defaultResume.setContent("Java, Spring Boot 경험이 있는 백엔드 개발자입니다. " +
                    "3년간 웹 애플리케이션 개발 경험이 있으며, RESTful API 설계 및 개발에 능숙합니다.");
            return resumeRepository.save(defaultResume);
        }

        return resumeRepository.findById(resumeId).orElseGet(() -> {
            log.warn("이력서를 찾을 수 없음 (ID: {}). 기본 이력서 생성", resumeId);
            Resume defaultResume = new Resume();
            defaultResume.setContent("백엔드 개발자 지원자입니다. 다양한 프로젝트 경험을 보유하고 있습니다.");
            return resumeRepository.save(defaultResume);
        });
    }

    /**
     * 포지션 조회 또는 생성
     */
    private Position getOrCreatePosition(Integer positionId) {
        if (positionId == null) {
            log.info("포지션 ID가 제공되지 않음. 기본 포지션 생성");
            Position defaultPosition = new Position();
            defaultPosition.setName("백엔드 개발자");
            return positionRepository.save(defaultPosition);
        }

        return positionRepository.findById(positionId).orElseGet(() -> {
            log.warn("포지션을 찾을 수 없음 (ID: {}). 기본 포지션 생성", positionId);
            Position defaultPosition = new Position();
            defaultPosition.setName("백엔드 개발자");
            return positionRepository.save(defaultPosition);
        });
    }

    /**
     * 질문 생성 또는 조회
     */
    private List<Question> generateOrRetrieveQuestions(CreateInterviewRequest request, Resume resume, Position position, InterviewSession session) {
        List<Question> questions;

        // 1. 특정 질문 ID들이 제공된 경우
        if (request.getQuestionIds() != null && !request.getQuestionIds().isEmpty()) {
            log.info("기존 질문 사용 - 질문 ID: {}", request.getQuestionIds());
            questions = request.getQuestionIds().stream()
                    .map(id -> questionRepository.findById(id)
                            .orElseThrow(() -> new QuestionNotFoundException(id)))
                    .collect(Collectors.toList());
            return questions;
        }

        // 2. 연습 모드인 경우
        if (InterviewMode.PRACTICE.equals(request.getMode())) {
            log.info("연습 모드 - 랜덤 질문 조회");
            questions = questionRepository.findRandomQuestions(
                    request.getType().toString(),
                    request.getQuestionCount());

            if (questions.isEmpty()) {
                log.warn("랜덤 질문을 찾을 수 없음. LLM 서비스로 폴백");
                return generateQuestionsWithLlm(resume, position, request.getQuestionCount());
            }
            return questions;
        }

        // 3. 실전 모드: LLM 서버 연동
        log.info("실전 모드 - LLM 서비스로 질문 생성");
        return generateQuestionsWithLlm(resume, position, request.getQuestionCount());
    }

    /**
     * LLM 서비스를 통한 질문 생성 (FastAPI 연동)
     */
    private List<Question> generateQuestionsWithLlm(Resume resume, Position position, int questionCount) {
        try {
            log.info("LLM 서비스 호출 - 포지션: {}, 질문 수: {}", position.getName(), questionCount);

            List<Question> questions = llmService.generateInterviewQuestions(resume, position, questionCount);

            if (questions == null || questions.isEmpty()) {
                throw new LLMServiceException("LLM 서비스에서 질문을 생성하지 못했습니다");
            }

            log.info("LLM 서비스로부터 {}개의 질문 생성 완료", questions.size());
            return questions;

        } catch (Exception e) {
            log.error("LLM 서비스 질문 생성 실패: {}. 폴백 처리 시작", e.getMessage());

            // 폴백: 랜덤 질문 사용
            List<Question> fallbackQuestions = questionRepository.findRandomQuestions("TECHNICAL", questionCount);

            if (fallbackQuestions.isEmpty()) {
                // 최후 수단: 하드코딩된 기본 질문
                return createDefaultQuestions(questionCount);
            }

            log.info("폴백으로 {}개의 랜덤 질문 사용", fallbackQuestions.size());
            return fallbackQuestions;
        }
    }

    /**
     * 기본 질문 생성 (최후 폴백)
     */
    private List<Question> createDefaultQuestions(int questionCount) {
        log.warn("기본 질문 생성 - 요청 수: {}", questionCount);

        List<String> defaultQuestions = List.of(
                "자기소개를 해주세요.",
                "이 회사에 지원한 이유가 무엇인가요?",
                "본인의 장점과 단점에 대해 말씀해주세요.",
                "최근 진행한 프로젝트에 대해 설명해주세요.",
                "앞으로의 커리어 계획은 어떻게 되나요?"
        );

        List<Question> questions = new ArrayList<>();
        for (int i = 0; i < Math.min(questionCount, defaultQuestions.size()); i++) {
            Question question = Question.builder()
                    .content(defaultQuestions.get(i))
                    .type(QuestionType.PERSONALITY)
                    .category("General")
                    .difficultyLevel(1)
                    .build();
            questions.add(question);
        }

        return questions;
    }

    /**
     * 질문을 세션에 할당
     */
    private void assignQuestionsToSession(List<Question> questions, InterviewSession session) {
        int sequence = 1;
        for (Question question : questions) {
            question.setInterviewSession(session);
            question.setSequence(sequence++);
            questionRepository.save(question);
        }
        session.setQuestions(questions);
    }

    @Transactional
    public void startInterview(Integer interviewId) {
        log.info("면접 시작 - ID: {}", interviewId);

        InterviewSession interviewSession = interviewSessionRepository.findById(interviewId)
                .orElseThrow(() -> new InterviewSessionNotFoundException(interviewId));

        interviewSession.setStatus(InterviewStatus.IN_PROGRESS);
        interviewSession.setStartTime(LocalDateTime.now());
        interviewSessionRepository.save(interviewSession);

        log.info("면접 상태 변경 완료 - ID: {}, 상태: IN_PROGRESS", interviewId);
    }

    @Transactional
    public void completeInterview(Integer interviewId) {
        log.info("면접 완료 - ID: {}", interviewId);

        InterviewSession interviewSession = interviewSessionRepository.findById(interviewId)
                .orElseThrow(() -> new InterviewSessionNotFoundException(interviewId));

        interviewSession.setStatus(InterviewStatus.COMPLETED);
        interviewSession.setEndTime(LocalDateTime.now());
        interviewSessionRepository.save(interviewSession);

        log.info("면접 상태 변경 완료 - ID: {}, 상태: COMPLETED", interviewId);
    }

    @Transactional(readOnly = true)
    public List<Question> getInterviewQuestions(Integer interviewId) {
        InterviewSession interviewSession = interviewSessionRepository.findById(interviewId)
                .orElseThrow(() -> new InterviewSessionNotFoundException(interviewId));

        return questionRepository.findByInterviewSessionOrderBySequenceAsc(interviewSession);
    }

    @Transactional
    public Answer submitTextAnswer(Integer questionId, String answerContent) {
        log.info("텍스트 답변 제출 - 질문 ID: {}", questionId);

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));

        try {
            // 개선된 LLM 서비스의 evaluateAnswer 메서드 사용
            Resume resume = question.getInterviewSession().getResume();
            Answer answer = llmService.evaluateAnswer(question, answerContent, resume);

            Answer savedAnswer = answerRepository.save(answer);
            log.info("답변 평가 완료 - 답변 ID: {}, 점수: 기술:{}, 의사소통:{}, 구조:{}",
                    savedAnswer.getId(),
                    savedAnswer.getTechnicalScore(),
                    savedAnswer.getCommunicationScore(),
                    savedAnswer.getStructureScore());

            return savedAnswer;

        } catch (Exception e) {
            log.error("LLM 서비스 피드백 생성 실패 - 질문 ID: {}, 오류: {}", questionId, e.getMessage());

            // 폴백: 기본 답변 생성
            Answer fallbackAnswer = createFallbackAnswer(question, answerContent);
            return answerRepository.save(fallbackAnswer);
        }
    }

    /**
     * 폴백 답변 생성
     */
    private Answer createFallbackAnswer(Question question, String answerContent) {
        log.warn("폴백 답변 생성 - 질문 ID: {}", question.getId());

        return Answer.builder()
                .question(question)
                .content(answerContent)
                .feedback("답변이 정상적으로 제출되었습니다. 자세한 평가는 나중에 제공될 예정입니다.")
                .communicationScore(75)
                .technicalScore(75)
                .structureScore(75)
                .build();
    }

    @Transactional
    public Answer submitAudioAnswer(Integer questionId, MultipartFile audioFile) {
        log.info("음성 답변 제출 - 질문 ID: {}, 파일: {}", questionId, audioFile.getOriginalFilename());

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));

        // TODO: 음성 인식 기능 구현 필요
        log.warn("음성 처리 기능이 아직 구현되지 않았습니다. 텍스트로 처리합니다.");

        String transcribedText = "음성 답변이 제출되었습니다: " + audioFile.getOriginalFilename();

        try {
            Resume resume = question.getInterviewSession().getResume();
            Answer answer = llmService.evaluateAnswer(question, transcribedText, resume);
            return answerRepository.save(answer);
        } catch (Exception e) {
            log.error("음성 답변 LLM 서비스 피드백 생성 실패", e);
            Answer fallbackAnswer = createFallbackAnswer(question, transcribedText);
            return answerRepository.save(fallbackAnswer);
        }
    }

    @Transactional
    public String getQuestionAudio(Integer questionId) {
        log.info("질문 음성 요청 - 질문 ID: {}", questionId);

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));

        // TODO: TTS 기능 구현 필요
        log.warn("Text-to-speech 서비스가 아직 구현되지 않았습니다.");
        return ""; // 실제로는 TTS 서비스를 통해 오디오 URL을 반환해야 함
    }

    @Transactional
    public String generateShareUrl(Integer interviewId) {
        InterviewSession interviewSession = interviewSessionRepository.findById(interviewId)
                .orElseThrow(() -> new InterviewSessionNotFoundException(interviewId));

        if (interviewSession.getShareUrl() == null) {
            interviewSession.setShareUrl(UUID.randomUUID().toString());
            interviewSessionRepository.save(interviewSession);
        }

        return interviewSession.getShareUrl();
    }

    @Transactional
    public Question getNextQuestion(Integer interviewId) {
        log.info("다음 질문 요청 - 면접 ID: {}", interviewId);

        InterviewSession session = interviewSessionRepository.findById(interviewId)
                .orElseThrow(() -> new InterviewSessionNotFoundException(interviewId));

        if (session.getCurrentQuestionIndex() >= session.getQuestionCount()) {
            throw new InterviewStateException("더 이상 사용 가능한 질문이 없습니다");
        }

        List<Question> questions = questionRepository.findByInterviewSessionOrderBySequenceAsc(session);

        if (questions.isEmpty()) {
            throw new QuestionNotFoundException("error.question.not.found", "면접에 등록된 질문이 없습니다");
        }

        if (session.getCurrentQuestionIndex() >= questions.size()) {
            throw new QuestionIndexOutOfBoundsException(session.getCurrentQuestionIndex(), questions.size());
        }

        Question nextQuestion = questions.get(session.getCurrentQuestionIndex());

        // 현재 질문 인덱스 업데이트
        session.setCurrentQuestionIndex(session.getCurrentQuestionIndex() + 1);
        interviewSessionRepository.save(session);

        log.info("다음 질문 반환 - 질문 ID: {}, 순서: {}", nextQuestion.getId(), nextQuestion.getSequence());
        return nextQuestion;
    }

    @Transactional
    public void updateInterviewTime(Integer interviewId, Integer timeInSeconds) {
        InterviewSession session = interviewSessionRepository.findById(interviewId)
                .orElseThrow(() -> new InterviewSessionNotFoundException(interviewId));

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
                .orElseThrow(() -> new UserNotFoundException(userId));

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));

        if (user.getFavoritedQuestions().contains(question)) {
            user.getFavoritedQuestions().remove(question);
            log.info("즐겨찾기에서 질문 제거 - 사용자: {}, 질문: {}", userId, questionId);
        } else {
            user.getFavoritedQuestions().add(question);
            log.info("즐겨찾기에 질문 추가 - 사용자: {}, 질문: {}", userId, questionId);
        }

        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<Question> getFavoriteQuestions(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        return new ArrayList<>(user.getFavoritedQuestions());
    }

    public User getUserById(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    public Resume getResumeById(Integer id) {
        return resumeRepository.findById(id).orElse(null);
    }

    public Position getPositionById(Integer id) {
        return positionRepository.findById(id).orElse(null);
    }

    /**
     * 면접에 질문 추가
     */
    @Transactional
    public Question addQuestionToInterview(Integer interviewId, Question question) {
        log.info("면접 {}에 질문 추가: {}", interviewId, question.getContent());

        InterviewSession interview = getInterviewById(interviewId);

        question.setInterviewSession(interview);
        question.setSequence(getNextQuestionSequence(interviewId));

        Question savedQuestion = questionRepository.save(question);
        log.info("질문 추가 완료 - 질문 ID: {}, 순서: {}", savedQuestion.getId(), savedQuestion.getSequence());

        return savedQuestion;
    }

    /**
     * 다음 질문 순서 가져오기
     */
    private Integer getNextQuestionSequence(Integer interviewId) {
        Integer maxSequence = questionRepository.findMaxSequenceByInterviewId(interviewId);
        return maxSequence != null ? maxSequence + 1 : 1;
    }

    /**
     * 질문 ID로 질문 조회
     */
    public Question getQuestionById(Integer questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));
    }

    /**
     * 답변 평가 결과 저장 (FastAPI에서 받은 평가 결과 처리)
     */
    @Transactional
    public void saveAnswerEvaluation(Integer answerId, AnswerEvaluation evaluationDto) {
        log.info("답변 {} 평가 결과 저장: 총점 {}", answerId, evaluationDto.getTotalScore());

        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new AnswerNotFoundException(answerId));

        // DTO를 Entity로 변환
        AnswerEvaluation evalEntity = AnswerEvaluation.builder()
                .answer(answer)
                .relevance(evaluationDto.getRelevance())
                .specificity(evaluationDto.getSpecificity())
                .practicality(evaluationDto.getPracticality())
                .validity(evaluationDto.getValidity())
                .totalScore(evaluationDto.getTotalScore())
                .feedback(evaluationDto.getFeedback())
                .evaluationType("AI_FASTAPI") // FastAPI AI 평가 표시
                .build();

        answerEvaluationRepository.save(evalEntity);
        log.info("답변 평가 저장 완료 - 평가 ID: {}", evalEntity.getId());
    }

    /**
     * 면접 시뮬레이션 결과 저장
     */
    @Transactional
    public void saveSimulationResult(Integer interviewId, InterviewSimulationResult result) {
        log.info("면접 {} 시뮬레이션 결과 저장", interviewId);

        InterviewSession interview = getInterviewById(interviewId);

        InterviewSimulation simulation = InterviewSimulation.builder()
                .interviewSession(interview)
                .generatedQuestions(String.join("|||", result.getGeneratedQuestions()))
                .selectedQuestion(result.getSelectedQuestion())
                .userAnswer(result.getUserAnswer())
                .build();

        // FastAPI에서 받은 평가 결과 저장
        if (result.getEvaluationResult() != null) {
            AnswerEvaluation eval = result.getEvaluationResult();
            simulation.setEvaluationRelevance(eval.getRelevance());
            simulation.setEvaluationSpecificity(eval.getSpecificity());
            simulation.setEvaluationPracticality(eval.getPracticality());
            simulation.setEvaluationValidity(eval.getValidity());
            simulation.setEvaluationTotalScore(eval.getTotalScore());
            simulation.setEvaluationFeedback(eval.getFeedback());
        }

        InterviewSimulation savedSimulation = interviewSimulationRepository.save(simulation);
        log.info("시뮬레이션 결과 저장 완료 - 시뮬레이션 ID: {}", savedSimulation.getId());
    }

    /**
     * FastAPI 서버 연결 상태 확인
     */
    public boolean checkLlmServiceHealth() {
        try {
            // 간단한 테스트 질문 생성 시도
            Resume testResume = new Resume();
            testResume.setContent("테스트 이력서");

            Position testPosition = new Position();
            testPosition.setName("테스트 포지션");

            List<Question> testQuestions = llmService.generateInterviewQuestions(testResume, testPosition, 1);

            boolean isHealthy = testQuestions != null && !testQuestions.isEmpty();
            log.info("LLM 서비스 상태 확인: {}", isHealthy ? "정상" : "비정상");

            return isHealthy;
        } catch (Exception e) {
            log.error("LLM 서비스 상태 확인 실패: {}", e.getMessage());
            return false;
        }
    }
}