package dev.swote.interv.domain.interview.DTO;

import dev.swote.interv.domain.interview.entity.Answer;
import dev.swote.interv.domain.interview.entity.AnswerEvaluation;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AnswerWithEvaluation {
    private Answer answer;
    private AnswerEvaluation evaluation;
}