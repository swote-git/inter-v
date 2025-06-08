package dev.swote.interv.service.ai;

import dev.swote.interv.domain.interview.entity.Answer;
import dev.swote.interv.domain.interview.entity.Question;
import dev.swote.interv.domain.interview.entity.QuestionType;
import dev.swote.interv.domain.position.entity.Position;
import dev.swote.interv.domain.resume.entity.Resume;
import dev.swote.interv.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmService {

    private final RestTemplate restTemplate;

    @Value("${llm.api.url}")
    private String apiUrl;

    @Value("${llm.api.key:}")
    private String apiKey;

    /**
     * FastAPI 서버에서 면접 질문 생성
     */
    public List<Question> generateInterviewQuestions(Resume resume, Position position, int count) {
        try {
            log.info("면접 질문 생성 요청 - 포지션: {}, 질문 수: {}", position.getName(), count);

            // 1. 요청 바디 구성
            Map<String, Object> requestBody = createQuestionRequest(resume, position, count);

            // 2. HTTP 헤더 설정
            HttpHeaders headers = createHeaders();

            // 3. HTTP 요청 생성
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            // 4. FastAPI 서버 호출
            String endpoint = apiUrl + "/interview/questions";
            log.debug("API 호출 URL: {}", endpoint);

            ResponseEntity<Map> responseEntity = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            // 5. 응답 처리
            return processQuestionResponse(responseEntity.getBody(), count);

        } catch (ResourceAccessException e) {
            log.error("ML 서버 연결 실패: {}", e.getMessage(), e);
            throw new MLConnectionException();
        } catch (HttpClientErrorException e) {
            log.error("클라이언트 오류 (4xx): {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new MLBadRequestException();
        } catch (HttpServerErrorException e) {
            log.error("서버 오류 (5xx): {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new MLServerErrorException();
        } catch (MLResponseParsingException | QuestionGenerationException e) {
            // 이미 적절한 커스텀 예외이므로 재던지기
            throw e;
        } catch (Exception e) {
            log.error("예상치 못한 오류 발생: {}", e.getMessage(), e);
            throw new LLMServiceException("error.llm.question.generation.failed");
        }
    }

    /**
     * FastAPI 서버에서 답변 평가
     */
    public Answer evaluateAnswer(Question question, String answerContent, Resume resume) {
        try {
            log.info("답변 평가 요청 - 질문 ID: {}", question.getId());

            // 1. 요청 바디 구성
            Map<String, Object> requestBody = createEvaluationRequest(question, answerContent, resume);

            // 2. HTTP 헤더 설정
            HttpHeaders headers = createHeaders();

            // 3. HTTP 요청 생성
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            // 4. FastAPI 서버 호출
            String endpoint = apiUrl + "/evaluate";

            ResponseEntity<Map> responseEntity = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            // 5. 응답 처리
            return processEvaluationResponse(responseEntity.getBody(), question, answerContent);

        } catch (ResourceAccessException e) {
            log.error("답변 평가 중 연결 실패: {}", e.getMessage(), e);
            return createDefaultAnswer(question, answerContent, "네트워크 연결 오류로 인해 기본 평가가 제공됩니다.");
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("답변 평가 중 HTTP 오류: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            return createDefaultAnswer(question, answerContent, "서버 오류로 인해 기본 평가가 제공됩니다.");
        } catch (Exception e) {
            log.error("답변 평가 중 예상치 못한 오류: {}", e.getMessage(), e);
            return createDefaultAnswer(question, answerContent, "평가 처리 중 오류가 발생하여 기본 평가가 제공됩니다.");
        }
    }

    /**
     * FastAPI 요청 바디 생성 (질문 생성용)
     */
    private Map<String, Object> createQuestionRequest(Resume resume, Position position, int count) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("resume", resume.getContent());
        requestBody.put("position", position.getName());
        requestBody.put("question_count", count);

        log.debug("질문 생성 요청 바디: resume 길이={}, position={}, count={}",
                resume.getContent().length(), position.getName(), count);
        return requestBody;
    }

    /**
     * FastAPI 요청 바디 생성 (답변 평가용)
     */
    private Map<String, Object> createEvaluationRequest(Question question, String answerContent, Resume resume) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("question", question.getContent());
        requestBody.put("answer", answerContent);
        requestBody.put("resume", resume.getContent());
        requestBody.put("cover_letter", ""); // 자기소개서가 없는 경우 빈 문자열

        log.debug("답변 평가 요청 바디: question 길이={}, answer 길이={}",
                question.getContent().length(), answerContent.length());
        return requestBody;
    }

    /**
     * HTTP 헤더 생성
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        // API 키가 있는 경우 인증 헤더 추가
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            headers.set("Authorization", "Bearer " + apiKey);
            log.debug("API 키가 설정되었습니다.");
        } else {
            log.debug("API 키가 설정되지 않았습니다.");
        }

        return headers;
    }

    /**
     * FastAPI 질문 생성 응답 처리
     */
    private List<Question> processQuestionResponse(Map<String, Object> responseBody, int expectedCount) {
        if (responseBody == null) {
            log.error("ML 서버 응답이 null입니다.");
            throw new MLResponseParsingException("error.llm.response.parsing.failed");
        }

        if (!responseBody.containsKey("questions")) {
            log.error("ML 응답에 'questions' 필드가 없습니다. 응답: {}", responseBody);
            throw new MLResponseParsingException("error.llm.response.parsing.failed");
        }

        Object questionsObj = responseBody.get("questions");
        if (!(questionsObj instanceof List)) {
            log.error("ML 응답의 'questions' 필드가 배열이 아닙니다. 타입: {}", questionsObj.getClass().getSimpleName());
            throw new MLResponseParsingException("error.llm.response.parsing.failed");
        }

        List<?> questionsList = (List<?>) questionsObj;
        if (questionsList.isEmpty()) {
            log.error("생성된 질문이 없습니다.");
            throw new QuestionGenerationException("error.llm.question.generation.failed");
        }

        List<Question> result = new ArrayList<>();
        int sequence = 1;

        for (Object questionObj : questionsList) {
            if (!(questionObj instanceof Map)) {
                log.warn("잘못된 질문 객체 형식: {}", questionObj);
                continue;
            }

            try {
                Map<String, Object> questionMap = (Map<String, Object>) questionObj;
                Question question = mapToQuestion(questionMap, sequence++);
                if (question != null) {
                    result.add(question);
                }
            } catch (Exception e) {
                log.warn("질문 매핑 실패: {}", e.getMessage());
            }
        }

        if (result.isEmpty()) {
            log.error("유효한 질문을 생성하지 못했습니다.");
            throw new QuestionGenerationException("error.llm.question.generation.failed");
        }

        log.info("성공적으로 {}개의 질문을 생성했습니다 (요청: {}개)", result.size(), expectedCount);
        return result;
    }

    /**
     * Map을 Question 엔티티로 변환
     */
    private Question mapToQuestion(Map<String, Object> questionMap, int sequence) {
        String content = extractStringField(questionMap, "content");
        if (content == null || content.trim().isEmpty()) {
            log.warn("질문 content가 비어있습니다: {}", questionMap);
            return null;
        }

        String typeStr = extractStringField(questionMap, "type");
        QuestionType type = parseQuestionType(typeStr);

        String category = extractStringField(questionMap, "category");
        if (category == null) category = "General";

        int difficultyLevel = extractIntField(questionMap, "difficultyLevel", 1);

        return Question.builder()
                .content(content.trim())
                .type(type)
                .category(category)
                .difficultyLevel(difficultyLevel)
                .sequence(sequence)
                .build();
    }

    /**
     * FastAPI 답변 평가 응답 처리
     */
    private Answer processEvaluationResponse(Map<String, Object> responseBody, Question question, String answerContent) {
        if (responseBody == null) {
            log.warn("평가 응답이 null입니다. 기본 평가를 생성합니다.");
            return createDefaultAnswer(question, answerContent, "평가 응답을 받지 못했습니다.");
        }

        int communicationScore = extractIntField(responseBody, "관련성", 70);
        int technicalScore = extractIntField(responseBody, "실무성", 70);
        int structureScore = extractIntField(responseBody, "구체성", 70);
        String feedback = extractStringField(responseBody, "피드백");

        if (feedback == null || feedback.trim().isEmpty()) {
            feedback = "답변이 제출되었습니다.";
        }

        log.debug("평가 결과 - 관련성: {}, 실무성: {}, 구체성: {}", communicationScore, technicalScore, structureScore);

        return Answer.builder()
                .question(question)
                .content(answerContent)
                .feedback(feedback)
                .communicationScore(communicationScore)
                .technicalScore(technicalScore)
                .structureScore(structureScore)
                .build();
    }

    /**
     * 평가 실패시 기본 답변 생성
     */
    private Answer createDefaultAnswer(Question question, String answerContent, String reason) {
        log.info("기본 답변 생성 - 이유: {}", reason);

        return Answer.builder()
                .question(question)
                .content(answerContent)
                .feedback("답변이 정상적으로 제출되었습니다. " + reason)
                .communicationScore(75)
                .technicalScore(75)
                .structureScore(75)
                .build();
    }

    // 유틸리티 메서드들
    private String extractStringField(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString().trim() : null;
    }

    private int extractIntField(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException e) {
                log.warn("정수 파싱 실패 - key: {}, value: {}, 기본값 사용: {}", key, value, defaultValue);
            }
        }
        return defaultValue;
    }

    private QuestionType parseQuestionType(String typeStr) {
        if (typeStr == null) {
            log.debug("질문 타입이 null이므로 기본값 TECHNICAL 사용");
            return QuestionType.TECHNICAL;
        }

        try {
            return QuestionType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("알 수 없는 질문 타입: {}, 기본값 TECHNICAL 사용", typeStr);
            return QuestionType.TECHNICAL;
        }
    }

    /**
     * ML 서버 상태 확인
     */
    public boolean isMLServerHealthy() {
        try {
            String healthEndpoint = apiUrl + "/health";
            ResponseEntity<String> response = restTemplate.getForEntity(healthEndpoint, String.class);
            boolean isHealthy = response.getStatusCode() == HttpStatus.OK;
            log.debug("ML 서버 상태 확인 - 정상: {}", isHealthy);
            return isHealthy;
        } catch (Exception e) {
            log.warn("ML 서버 상태 확인 실패: {}", e.getMessage());
            return false;
        }
    }
}