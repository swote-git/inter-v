// 면접 서비스 상태 확인 및 테스트용 컨트롤러
package dev.swote.interv.controller.admin;

import dev.swote.interv.service.interview.InterviewService;
import dev.swote.interv.service.ai.LlmService;
import dev.swote.interv.domain.interview.entity.Question;
import dev.swote.interv.domain.resume.entity.Resume;
import dev.swote.interv.domain.position.entity.Position;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/health")
@RequiredArgsConstructor
public class HealthCheckController {

    private final InterviewService interviewService;
    private final LlmService llmService;

    /**
     * 전체 시스템 상태 확인
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();

        try {
            // LLM 서비스 상태 확인
            boolean llmHealthy = interviewService.checkLlmServiceHealth();

            status.put("timestamp", LocalDateTime.now());
            status.put("status", llmHealthy ? "UP" : "DOWN");
            status.put("services", Map.of(
                    "interview_service", "UP",
                    "llm_service", llmHealthy ? "UP" : "DOWN",
                    "fastapi_connection", llmHealthy ? "CONNECTED" : "DISCONNECTED"
            ));

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            log.error("시스템 상태 확인 실패", e);

            status.put("timestamp", LocalDateTime.now());
            status.put("status", "ERROR");
            status.put("error", e.getMessage());

            return ResponseEntity.status(500).body(status);
        }
    }

    /**
     * LLM 서비스 연결 테스트
     */
    @PostMapping("/llm/test")
    public ResponseEntity<Map<String, Object>> testLlmConnection(@RequestBody(required = false) Map<String, String> testData) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 테스트 데이터 준비
            String resumeContent = testData != null ?
                    testData.getOrDefault("resume", "Java, Spring Boot 개발자입니다.") :
                    "Java, Spring Boot, RESTful API 개발 경험이 있는 백엔드 개발자입니다.";

            String positionName = testData != null ?
                    testData.getOrDefault("position", "백엔드 개발자") :
                    "백엔드 개발자";

            Resume testResume = new Resume();
            testResume.setContent(resumeContent);

            Position testPosition = new Position();
            testPosition.setName(positionName);

            // LLM 서비스 테스트
            long startTime = System.currentTimeMillis();
            List<Question> questions = llmService.generateInterviewQuestions(testResume, testPosition, 3);
            long responseTime = System.currentTimeMillis() - startTime;

            result.put("success", true);
            result.put("response_time_ms", responseTime);
            result.put("questions_generated", questions.size());
            result.put("sample_question", questions.isEmpty() ? null : questions.get(0).getContent());
            result.put("test_data", Map.of(
                    "resume_length", resumeContent.length(),
                    "position", positionName
            ));

            log.info("LLM 연결 테스트 성공 - 응답시간: {}ms, 질문 수: {}", responseTime, questions.size());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("LLM 연결 테스트 실패", e);

            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("error_type", e.getClass().getSimpleName());

            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * FastAPI 서버 핑 테스트
     */
    @GetMapping("/fastapi/ping")
    public ResponseEntity<Map<String, Object>> pingFastApi() {
        Map<String, Object> result = new HashMap<>();

        try {
            // 간단한 연결 테스트
            boolean isHealthy = interviewService.checkLlmServiceHealth();

            result.put("fastapi_reachable", isHealthy);
            result.put("timestamp", LocalDateTime.now());
            result.put("message", isHealthy ? "FastAPI 서버 연결 정상" : "FastAPI 서버 연결 실패");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("FastAPI 핑 테스트 실패", e);

            result.put("fastapi_reachable", false);
            result.put("error", e.getMessage());
            result.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 서비스 메트릭스 조회
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getServiceMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        try {
            // 여기서 실제 메트릭스를 수집할 수 있습니다
            // 예: 데이터베이스 연결 수, 활성 세션 수, 평균 응답 시간 등

            metrics.put("system", Map.of(
                    "uptime", "정상",
                    "memory_usage", "양호",
                    "database_connections", "정상"
            ));

            metrics.put("llm_service", Map.of(
                    "status", interviewService.checkLlmServiceHealth() ? "정상" : "비정상",
                    "last_check", LocalDateTime.now()
            ));

            return ResponseEntity.ok(metrics);

        } catch (Exception e) {
            log.error("메트릭스 조회 실패", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}