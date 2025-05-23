package dev.swote.interv.service.interview.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {
    private String content;
    private String type;
    private String category;
    private Integer difficultyLevel;
}