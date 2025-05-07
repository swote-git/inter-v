package dev.swote.interv.service.ai;

import dev.swote.interv.domain.interview.entity.Answer;
import dev.swote.interv.domain.interview.entity.Question;
import dev.swote.interv.domain.interview.entity.QuestionType;
import dev.swote.interv.domain.position.entity.Position;
import dev.swote.interv.domain.resume.entity.Resume;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmService {

    private final RestTemplate restTemplate;

    @Value("${llm.api.url}")
    private String apiUrl;

    @Value("${llm.api.key}")
    private String apiKey;

    // Question categories for technical interviews
    private static final List<String> TECH_CATEGORIES = Arrays.asList(
            "JavaScript", "React", "Java", "Spring", "Python", "SQL", "Data Structures",
            "Algorithms", "System Design", "API Design", "Microservices", "DevOps"
    );

    // Question categories for non-technical interviews
    private static final List<String> NON_TECH_CATEGORIES = Arrays.asList(
            "Project Experience", "Teamwork", "Leadership", "Problem Solving",
            "Communication", "Conflict Resolution", "Career Goals"
    );

    public List<Question> generateInterviewQuestions(Resume resume, Position position, int count) {
        List<Question> questions = new ArrayList<>();

        // If we're generating from AI, we'd call the API here
        // For demo purposes, let's generate some sample questions

        Random random = new Random();

        for (int i = 0; i < count; i++) {
            QuestionType questionType;
            String category;
            String content;

            // Determine question type
            int typeRand = random.nextInt(4);
            switch (typeRand) {
                case 0:
                    questionType = QuestionType.TECHNICAL;
                    category = TECH_CATEGORIES.get(random.nextInt(TECH_CATEGORIES.size()));
                    content = generateTechnicalQuestion(category);
                    break;
                case 1:
                    questionType = QuestionType.PERSONALITY;
                    category = NON_TECH_CATEGORIES.get(random.nextInt(NON_TECH_CATEGORIES.size()));
                    content = generatePersonalityQuestion(category);
                    break;
                case 2:
                    questionType = QuestionType.PROJECT;
                    category = "Project Experience";
                    content = generateProjectQuestion(resume);
                    break;
                default:
                    questionType = QuestionType.SITUATION;
                    category = "Situational";
                    content = generateSituationalQuestion();
                    break;
            }

            Question question = Question.builder()
                    .content(content)
                    .type(questionType)
                    .category(category)
                    .difficultyLevel(random.nextInt(3) + 1) // 1-3 difficulty
                    .sequence(i + 1)
                    .build();

            questions.add(question);
        }

        return questions;
    }

    private String generateTechnicalQuestion(String category) {
        Map<String, List<String>> technicalQuestions = new HashMap<>();

        technicalQuestions.put("JavaScript", Arrays.asList(
                "What is the difference between let, const, and var in JavaScript?",
                "Explain how closures work in JavaScript.",
                "How does prototypal inheritance work in JavaScript?",
                "What's the difference between synchronous and asynchronous code in JavaScript?"
        ));

        technicalQuestions.put("React", Arrays.asList(
                "React의 Virtual DOM에 대해 설명해주세요.",
                "React 컴포넌트의 라이프사이클에 대해 설명해주세요.",
                "React에서 상태 관리는 어떻게 하나요?",
                "React Hooks의 장점은 무엇인가요?"
        ));

        technicalQuestions.put("Java", Arrays.asList(
                "Java에서 인터페이스와 추상 클래스의 차이점은 무엇인가요?",
                "Java의 가비지 컬렉션은 어떻게 작동하나요?",
                "Java에서 스레드 안전성을 확보하는 방법은 무엇인가요?",
                "Java 8의 주요 기능에 대해 설명해주세요."
        ));

        // Add more categories as needed

        List<String> questions = technicalQuestions.getOrDefault(category,
                Collections.singletonList("Can you explain your experience with " + category + "?"));

        return questions.get(new Random().nextInt(questions.size()));
    }

    private String generatePersonalityQuestion(String category) {
        Map<String, List<String>> personalityQuestions = new HashMap<>();

        personalityQuestions.put("Teamwork", Arrays.asList(
                "팀 프로젝트에서 갈등이 발생했을 때 어떻게 해결하셨나요?",
                "팀원들과 효과적으로 협업하기 위해 어떤 전략을 사용하시나요?",
                "팀에서 맡았던 역할에 대해 설명해주세요."
        ));

        personalityQuestions.put("Leadership", Arrays.asList(
                "리더십을 발휘한 경험에 대해 이야기해주세요.",
                "다른 사람들을 어떻게 동기부여하시나요?",
                "어려운 결정을 내려야 했던 상황에 대해 설명해주세요."
        ));

        // Add more categories

        List<String> questions = personalityQuestions.getOrDefault(category,
                Collections.singletonList("Can you tell me about your " + category + " skills?"));

        return questions.get(new Random().nextInt(questions.size()));
    }

    private String generateProjectQuestion(Resume resume) {
        List<String> projectQuestions = Arrays.asList(
                "가장 도전적이었던 프로젝트에 대해 설명해주세요.",
                "프로젝트에서 어떤 기술적 문제를 해결하셨나요?",
                "프로젝트에서 배운 가장 중요한 교훈은 무엇인가요?",
                "프로젝트에서 사용한 기술 스택을 선택한 이유는 무엇인가요?"
        );

        return projectQuestions.get(new Random().nextInt(projectQuestions.size()));
    }

    private String generateSituationalQuestion() {
        List<String> situationalQuestions = Arrays.asList(
                "마감 시간이 임박한 상황에서 어떻게 대처하시나요?",
                "동료가 실수를 했을 때 어떻게 대응하시나요?",
                "상사와 의견이 다를 때 어떻게 대처하시나요?",
                "새로운 기술을 배워야 하는 상황에서 어떻게 접근하시나요?"
        );

        return situationalQuestions.get(new Random().nextInt(situationalQuestions.size()));
    }

    public Answer provideFeedback(Question question, String answerContent) {
        // In a real implementation, we'd call an LLM API for scoring and feedback
        // For demo purposes, let's create some basic feedback

        Random random = new Random();

        // Generate scores between 70-100
        int communicationScore = random.nextInt(31) + 70;
        int technicalScore = random.nextInt(31) + 70;
        int structureScore = random.nextInt(31) + 70;

        // Generate feedback based on question type
        String feedback;
        switch (question.getType()) {
            case TECHNICAL:
                feedback = "기술적인 이해도가 돋보입니다. " +
                        (technicalScore > 90 ? "핵심 개념을 정확히 파악하셨습니다." :
                                "몇 가지 핵심 개념을 추가로 언급하시면 더 좋을 것 같습니다.");
                break;
            case PERSONALITY:
                feedback = "자신의 경험을 잘 표현하셨습니다. " +
                        (communicationScore > 90 ? "구체적인 사례를 통해 설득력 있게 전달하셨습니다." :
                                "더 구체적인 사례를 추가하시면 좋을 것 같습니다.");
                break;
            case PROJECT:
                feedback = "프로젝트 경험을 잘 설명해주셨습니다. " +
                        (structureScore > 90 ? "프로젝트의 목표, 과정, 결과를 체계적으로 설명하셨습니다." :
                                "프로젝트의 결과와 배운 점을 더 강조하시면 좋을 것 같습니다.");
                break;
            default:
                feedback = "답변이 명확하고 논리적입니다. " +
                        (communicationScore + technicalScore + structureScore > 270 ?
                                "전반적으로 훌륭한 답변입니다." :
                                "조금 더 체계적으로 답변을 구성하시면 좋을 것 같습니다.");
        }

        return Answer.builder()
                .question(question)
                .content(answerContent)
                .feedback(feedback)
                .communicationScore(communicationScore)
                .technicalScore(technicalScore)
                .structureScore(structureScore)
                .build();
    }
}