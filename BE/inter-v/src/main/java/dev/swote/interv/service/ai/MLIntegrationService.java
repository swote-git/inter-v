package dev.swote.interv.service.ai;

import dev.swote.interv.domain.interview.entity.AnswerEvaluation;
import dev.swote.interv.domain.interview.entity.Question;
import dev.swote.interv.domain.interview.DTO.InterviewSimulationResult;
import dev.swote.interv.domain.interview.entity.QuestionType;
import dev.swote.interv.exception.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class MLIntegrationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${llm.api.url}")
    private String mlApiBaseUrl;

    @Value("${llm.api.key:dummy-api-key}")
    private String apiKey;

    /**
     * ML API를 통한 면접 질문 생성
     */
    public List<Question> generateInterviewQuestions(String resumeContent, String position, int questionCount) {
        try {
            log.info("ML API 면접 질문 생성 요청 - 포지션: {}, 질문 수: {}", position, questionCount);

            String url = mlApiBaseUrl + "/interview/questions";

            // 요청 데이터 구성
            Map<String, Object> requestBody = Map.of(
                    "resume", resumeContent,
                    "position", position,
                    "questionCount", questionCount
            );

            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // ML API 호출
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return processQuestionResponse(response.getBody());
            }

            log.warn("ML API 응답이 비어있음");
            return generateFallbackQuestions(position, questionCount);

        } catch (ResourceAccessException e) {
            log.error("ML API 연결 실패 - 네트워크 오류: {}", e.getMessage(), e);
            // 폴백으로 처리하지만 연결 실패 로그는 남김
            return generateFallbackQuestions(position, questionCount);
        } catch (HttpClientErrorException e) {
            log.error("ML API 호출 실패 - 클라이언트 오류: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new MLBadRequestException();
        } catch (HttpServerErrorException e) {
            log.error("ML API 호출 실패 - 서버 오류: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new MLServerErrorException();
        } catch (MLResponseParsingException e) {
            // 이미 적절한 예외이므로 재던지기
            throw e;
        } catch (Exception e) {
            log.error("ML API 호출 중 예상치 못한 오류", e);
            throw new QuestionGenerationException();
        }
    }

    /**
     * ML API를 통한 답변 평가
     */
    public AnswerEvaluation evaluateAnswer(String question, String answer, String resumeContent, String coverLetter) {
        try {
            log.info("ML API 답변 평가 요청 - 질문 길이: {}, 답변 길이: {}", question.length(), answer.length());

            String url = mlApiBaseUrl + "/evaluate";

            // 요청 데이터 구성
            Map<String, Object> requestBody = Map.of(
                    "question", question,
                    "answer", answer,
                    "resume", resumeContent != null ? resumeContent : "",
                    "cover_letter", coverLetter != null ? coverLetter : ""
            );

            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // ML API 호출
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                AnswerEvaluation evaluation = convertToAnswerEvaluation(response.getBody());
                log.info("ML API 답변 평가 성공 - 총점: {}", evaluation.getTotalScore());
                return evaluation;
            }

            log.warn("ML API 평가 응답이 비어있음");
            return generateFallbackEvaluation();

        } catch (ResourceAccessException e) {
            log.error("답변 평가 중 연결 실패: {}", e.getMessage(), e);
            return generateFallbackEvaluation();
        } catch (HttpClientErrorException e) {
            log.error("답변 평가 중 클라이언트 오류: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            return generateFallbackEvaluation();
        } catch (HttpServerErrorException e) {
            log.error("답변 평가 중 서버 오류: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            return generateFallbackEvaluation();
        } catch (Exception e) {
            log.error("ML API 답변 평가 중 예상치 못한 오류", e);
            return generateFallbackEvaluation();
        }
    }

    /**
     * ML API를 통한 키워드 유사도 계산
     */
    public Map<String, Object> calculateKeywordSimilarity(String resumeContent, String coverLetter, String question) {
        try {
            log.info("ML API 키워드 유사도 계산 요청");

            String url = mlApiBaseUrl + "/similarity/keyword";

            Map<String, Object> requestBody = Map.of(
                    "resume", resumeContent,
                    "cover_letter", coverLetter != null ? coverLetter : "",
                    "question", question
            );

            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("키워드 유사도 계산 성공");
                return response.getBody();
            }

            return getDefaultSimilarityResult();

        } catch (ResourceAccessException e) {
            log.error("키워드 유사도 계산 중 연결 실패: {}", e.getMessage(), e);
            return getDefaultSimilarityResult();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("키워드 유사도 계산 중 HTTP 오류: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            return getDefaultSimilarityResult();
        } catch (Exception e) {
            log.error("키워드 유사도 계산 중 예상치 못한 오류", e);
            return getDefaultSimilarityResult();
        }
    }

    /**
     * ML API를 통한 의미론적 유사도 계산
     */
    public Map<String, Object> calculateSemanticSimilarity(String resumeContent, String coverLetter, String question) {
        try {
            log.info("ML API 의미론적 유사도 계산 요청");

            String url = mlApiBaseUrl + "/similarity/semantic";

            Map<String, Object> requestBody = Map.of(
                    "resume", resumeContent,
                    "cover_letter", coverLetter != null ? coverLetter : "",
                    "question", question
            );

            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("의미론적 유사도 계산 성공");
                return response.getBody();
            }

            return Map.of("similarity_score", 0.0);

        } catch (ResourceAccessException e) {
            log.error("의미론적 유사도 계산 중 연결 실패: {}", e.getMessage(), e);
            return Map.of("similarity_score", 0.0);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("의미론적 유사도 계산 중 HTTP 오류: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            return Map.of("similarity_score", 0.0);
        } catch (Exception e) {
            log.error("의미론적 유사도 계산 중 예상치 못한 오류", e);
            return Map.of("similarity_score", 0.0);
        }
    }

    /**
     * ML API를 통한 면접 시뮬레이션
     */
    public InterviewSimulationResult simulateInterview(String resumeContent, String coverLetter,
                                                       String jobDescription, String userAnswer, int numQuestions) {
        try {
            log.info("ML API 면접 시뮬레이션 요청 - 질문 수: {}", numQuestions);

            String url = mlApiBaseUrl + "/simulate/simulate";

            Map<String, Object> requestBody = Map.of(
                    "resume", resumeContent,
                    "cover_letter", coverLetter != null ? coverLetter : "",
                    "job_description", jobDescription,
                    "user_answer", userAnswer,
                    "num_questions", numQuestions
            );

            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                InterviewSimulationResult result = convertToSimulationResult(response.getBody());
                log.info("면접 시뮬레이션 성공");
                return result;
            }

            return generateFallbackSimulationResult();

        } catch (ResourceAccessException e) {
            log.error("면접 시뮬레이션 중 연결 실패: {}", e.getMessage(), e);
            return generateFallbackSimulationResult();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("면접 시뮬레이션 중 HTTP 오류: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            return generateFallbackSimulationResult();
        } catch (Exception e) {
            log.error("면접 시뮬레이션 중 예상치 못한 오류", e);
            return generateFallbackSimulationResult();
        }
    }

    /**
     * ML 서버 상태 확인
     */
    public boolean isMLServerHealthy() {
        try {
            String url = mlApiBaseUrl + "/health";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            boolean isHealthy = response.getStatusCode() == HttpStatus.OK;
            log.debug("ML 서버 상태 확인 - 정상: {}", isHealthy);
            return isHealthy;
        } catch (Exception e) {
            log.warn("ML 서버 상태 확인 실패: {}", e.getMessage());
            return false;
        }
    }

    // ================================================================================
    // 유틸리티 메서드들
    // ================================================================================

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        if (apiKey != null && !apiKey.equals("dummy-api-key")) {
            headers.set("Authorization", "Bearer " + apiKey);
        }
        return headers;
    }

    private List<Question> processQuestionResponse(Map<String, Object> responseBody) {
        if (!responseBody.containsKey("questions")) {
            log.error("ML 응답에 'questions' 필드가 없습니다.");
            throw new MLResponseParsingException();
        }

        Object questionsObj = responseBody.get("questions");
        if (!(questionsObj instanceof List)) {
            log.error("ML 응답의 'questions' 필드가 배열이 아닙니다.");
            throw new MLResponseParsingException();
        }

        List<?> questionsList = (List<?>) questionsObj;
        if (questionsList.isEmpty()) {
            log.error("생성된 질문이 없습니다.");
            throw new QuestionGenerationException();
        }

        List<Question> result = new ArrayList<>();
        int sequence = 1;

        for (Object questionObj : questionsList) {
            try {
                Map<String, Object> questionMap = (Map<String, Object>) questionObj;
                Question question = convertToQuestion(questionMap);
                if (question != null) {
                    question.setSequence(sequence++);
                    result.add(question);
                }
            } catch (Exception e) {
                log.warn("질문 변환 실패: {}, 원인: {}", questionObj, e.getMessage());
            }
        }

        if (result.isEmpty()) {
            log.error("유효한 질문을 생성하지 못했습니다.");
            throw new QuestionGenerationException();
        }

        return result;
    }

    private Question convertToQuestion(Map<String, Object> questionData) {
        Object contentObj = questionData.get("content");
        if (contentObj == null || contentObj.toString().trim().isEmpty()) {
            log.warn("질문 content가 비어 있음: {}", questionData);
            return null;
        }

        String content = contentObj.toString().trim();
        String typeStr = (String) questionData.get("type");
        String category = (String) questionData.getOrDefault("category", "General");
        Object levelObj = questionData.get("difficultyLevel");

        QuestionType type = QuestionType.TECHNICAL;
        if (typeStr != null) {
            try {
                type = QuestionType.valueOf(typeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("알 수 없는 type 값 '{}', 기본값 TECHNICAL 사용", typeStr);
            }
        }

        int difficultyLevel = 1;
        if (levelObj instanceof Number) {
            difficultyLevel = ((Number) levelObj).intValue();
        } else if (levelObj != null) {
            try {
                difficultyLevel = Integer.parseInt(levelObj.toString());
            } catch (NumberFormatException e) {
                log.warn("difficultyLevel 파싱 실패: {}, 기본값 1 사용", levelObj);
            }
        }

        return Question.builder()
                .content(content)
                .type(type)
                .category(category)
                .difficultyLevel(difficultyLevel)
                .build();
    }

    private AnswerEvaluation convertToAnswerEvaluation(Map<String, Object> responseData) {
        AnswerEvaluation evaluation = new AnswerEvaluation();

        evaluation.setRelevance(getIntegerValue(responseData, "관련성"));
        evaluation.setSpecificity(getIntegerValue(responseData, "구체성"));
        evaluation.setPracticality(getIntegerValue(responseData, "실무성"));
        evaluation.setValidity(getIntegerValue(responseData, "유효성"));
        evaluation.setTotalScore(getIntegerValue(responseData, "총점"));
        evaluation.setFeedback((String) responseData.getOrDefault("피드백", "평가를 완료했습니다."));

        return evaluation;
    }

    private InterviewSimulationResult convertToSimulationResult(Map<String, Object> responseData) {
        InterviewSimulationResult result = new InterviewSimulationResult();

        result.setGeneratedQuestions((List<String>) responseData.get("generated_questions"));
        result.setSelectedQuestion((String) responseData.get("selected_question"));
        result.setUserAnswer((String) responseData.get("user_answer"));

        // 평가 결과 변환
        Map<String, Object> evalData = (Map<String, Object>) responseData.get("evaluation_result");
        if (evalData != null) {
            result.setEvaluationResult(convertToAnswerEvaluation(evalData));
        }

        return result;
    }

    private int getIntegerValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                log.warn("정수 변환 실패 - key: {}, value: {}", key, value);
                return 0;
            }
        }
        return 0;
    }

    private Map<String, Object> getDefaultSimilarityResult() {
        return Map.of("matched_keywords", List.of(), "keyword_match_score", 0.0);
    }

    // ================================================================================
    // 폴백 메서드들 (ML API 실패 시 사용)
    // ================================================================================

    private List<Question> generateFallbackQuestions(String position, int questionCount) {
        log.warn("ML API 실패로 인한 폴백 질문 생성 - 포지션: {}", position);

        List<String> fallbackQuestions = List.of(
                "자신의 강점과 약점에 대해 설명해주세요.",
                "이 포지션에 지원하게 된 동기는 무엇인가요?",
                "팀워크 경험에 대해 구체적인 사례를 들어 설명해주세요.",
                "가장 도전적이었던 프로젝트 경험을 말씀해주세요.",
                "기술적인 문제를 해결했던 경험이 있다면 공유해주세요.",
                "향후 커리어 계획에 대해 말씀해주세요.",
                "새로운 기술을 학습하는 방법에 대해 설명해주세요.",
                "스트레스 상황에서 어떻게 대처하시나요?",
                "리더십을 발휘했던 경험이 있다면 말씀해주세요.",
                "우리 회사에 대해 어떻게 생각하시나요?"
        );

        List<Question> questions = new ArrayList<>();

        for (int i = 0; i < Math.min(questionCount, fallbackQuestions.size()); i++) {
            Question question = Question.builder()
                    .content(fallbackQuestions.get(i))
                    .type(QuestionType.PERSONALITY)
                    .category("일반")
                    .difficultyLevel(2)
                    .sequence(i + 1)
                    .build();
            questions.add(question);
        }

        return questions;
    }

    private AnswerEvaluation generateFallbackEvaluation() {
        log.warn("ML API 실패로 인한 폴백 평가 생성");

        AnswerEvaluation evaluation = new AnswerEvaluation();
        evaluation.setRelevance(7);
        evaluation.setSpecificity(6);
        evaluation.setPracticality(7);
        evaluation.setValidity(7);
        evaluation.setTotalScore(27);
        evaluation.setFeedback("답변이 제출되었습니다. 상세한 평가를 위해 잠시 후 다시 확인해주세요.");

        return evaluation;
    }

    private InterviewSimulationResult generateFallbackSimulationResult() {
        log.warn("ML API 실패로 인한 폴백 시뮬레이션 결과 생성");

        InterviewSimulationResult result = new InterviewSimulationResult();
        result.setGeneratedQuestions(List.of("면접 시뮬레이션 질문을 준비 중입니다."));
        result.setSelectedQuestion("면접 시뮬레이션 질문을 준비 중입니다.");
        result.setUserAnswer("");
        result.setEvaluationResult(generateFallbackEvaluation());

        return result;
    }
}