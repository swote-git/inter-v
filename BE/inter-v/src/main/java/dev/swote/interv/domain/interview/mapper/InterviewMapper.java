package dev.swote.interv.domain.interview.mapper;

import dev.swote.interv.domain.interview.dto.*;
import dev.swote.interv.domain.interview.entity.*;
import dev.swote.interv.domain.resume.dto.UserSimpleResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class InterviewMapper {

    /**
     * InterviewSession -> InterviewListResponse 변환
     */
    public InterviewListResponse toListResponse(InterviewSession session) {
        if (session == null) {
            return null;
        }

        return InterviewListResponse.builder()
                .id(session.getId())
                .user(toUserSimpleResponse(session.getUser()))
                .resume(toResumeSimpleResponse(session.getResume()))
                .position(toPositionSimpleResponse(session.getPosition()))
                .type(session.getType())
                .mode(session.getMode())
                .status(session.getStatus())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .questionCount(session.getQuestionCount())
                .answeredQuestionCount(getAnsweredQuestionCount(session))
                .createdAt(session.getCreatedAt())
                .build();
    }

    /**
     * InterviewSession -> InterviewResponse 변환
     */
    public InterviewResponse toResponse(InterviewSession session) {
        if (session == null) {
            return null;
        }

        return InterviewResponse.builder()
                .id(session.getId())
                .user(toUserSimpleResponse(session.getUser()))
                .resume(toResumeSimpleResponse(session.getResume()))
                .position(toPositionSimpleResponse(session.getPosition()))
                .type(session.getType())
                .mode(session.getMode())
                .status(session.getStatus())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .shareUrl(session.getShareUrl())
                .questionCount(session.getQuestionCount())
                .currentQuestionIndex(session.getCurrentQuestionIndex())
                .totalTimeSeconds(session.getTotalTimeSeconds())
                .questions(toQuestionResponseList(session.getQuestions()))
                .createdAt(session.getCreatedAt())
                .build();
    }

    /**
     * Question -> QuestionResponse 변환
     */
    public QuestionResponse toQuestionResponse(Question question) {
        if (question == null) {
            return null;
        }

        return QuestionResponse.builder()
                .id(question.getId())
                .content(question.getContent())
                .type(question.getType())
                .sequence(question.getSequence())
                .difficultyLevel(question.getDifficultyLevel())
                .category(question.getCategory())
                .subCategory(question.getSubCategory())
                .answer(toAnswerResponse(question.getAnswer()))
                .build();
    }

    /**
     * Answer -> AnswerResponse 변환
     */
    public AnswerResponse toAnswerResponse(Answer answer) {
        if (answer == null) {
            return null;
        }

        return AnswerResponse.builder()
                .id(answer.getId())
                .content(answer.getContent())
                .feedback(answer.getFeedback())
                .audioFilePath(answer.getAudioFilePath())
                .communicationScore(answer.getCommunicationScore())
                .technicalScore(answer.getTechnicalScore())
                .structureScore(answer.getStructureScore())
                .build();
    }

    /**
     * User -> UserSimpleResponse 변환
     */
    private UserSimpleResponse toUserSimpleResponse(dev.swote.interv.domain.user.entity.User user) {
        if (user == null) {
            return null;
        }

        return UserSimpleResponse.builder()
                .id(user.getId())
                .userName(user.getUserName())
                .nickname(user.getNickname())
                .birthDate(user.getBirthDate())
                .build();
    }

    /**
     * Resume -> ResumeSimpleResponse 변환
     */
    private ResumeSimpleResponse toResumeSimpleResponse(dev.swote.interv.domain.resume.entity.Resume resume) {
        if (resume == null) {
            return null;
        }

        return ResumeSimpleResponse.builder()
                .id(resume.getId())
                .title(resume.getTitle())
                .objective(resume.getObjective())
                .build();
    }

    /**
     * Position -> PositionSimpleResponse 변환
     */
    private PositionSimpleResponse toPositionSimpleResponse(dev.swote.interv.domain.position.entity.Position position) {
        if (position == null) {
            return null;
        }

        return PositionSimpleResponse.builder()
                .id(position.getId())
                .name(position.getName())
                .title(position.getTitle())
                .company(toCompanySimpleResponse(position.getCompany()))
                .build();
    }

    /**
     * Company -> CompanySimpleResponse 변환
     */
    private CompanySimpleResponse toCompanySimpleResponse(dev.swote.interv.domain.company.entity.Company company) {
        if (company == null) {
            return null;
        }

        return CompanySimpleResponse.builder()
                .id(company.getId())
                .name(company.getName())
                .industry(company.getIndustry())
                .build();
    }

    /**
     * Question List -> QuestionResponse List 변환
     */
    private List<QuestionResponse> toQuestionResponseList(List<Question> questions) {
        if (questions == null) {
            return null;
        }

        return questions.stream()
                .map(this::toQuestionResponse)
                .collect(Collectors.toList());
    }

    /**
     * 답변 완료한 질문 수 계산
     */
    private Integer getAnsweredQuestionCount(InterviewSession session) {
        if (session.getQuestions() == null) {
            return 0;
        }

        return (int) session.getQuestions().stream()
                .filter(question -> question.getAnswer() != null)
                .count();
    }

    /**
     * AnswerWithEvaluation 생성
     */
    public AnswerWithEvaluation toAnswerWithEvaluation(Answer answer, AnswerEvaluation evaluation) {
        return new AnswerWithEvaluation(answer, evaluation);
    }
}