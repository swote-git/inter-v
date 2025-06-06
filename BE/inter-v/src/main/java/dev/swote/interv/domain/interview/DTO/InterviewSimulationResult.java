package dev.swote.interv.domain.interview.DTO;


import dev.swote.interv.controller.InterviewController;
import dev.swote.interv.domain.interview.entity.AnswerEvaluation;

import java.util.List;

@lombok.Data
public class InterviewSimulationResult {
    private List<String> generatedQuestions;
    private String selectedQuestion;
    private String userAnswer;
    private AnswerEvaluation evaluationResult;
}