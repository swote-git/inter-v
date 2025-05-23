package dev.swote.interv.service.interview.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterviewResponse {
    private Long id;
    private String type;
    private String mode;
    private String status;
    private String shareUrl;
    private List<QuestionResponse> questions;
}