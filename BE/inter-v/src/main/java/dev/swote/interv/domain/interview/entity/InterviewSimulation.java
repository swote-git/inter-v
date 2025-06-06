package dev.swote.interv.domain.interview.entity;

import dev.swote.interv.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "interview_simulations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewSimulation extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "interview_session_id")
    private InterviewSession interviewSession;

    @Column(name = "generated_questions", columnDefinition = "TEXT")
    private String generatedQuestions;

    @Column(name = "selected_question", columnDefinition = "TEXT")
    private String selectedQuestion;

    @Column(name = "user_answer", columnDefinition = "TEXT")
    private String userAnswer;

    @Column(name = "evaluation_relevance")
    private Integer evaluationRelevance;

    @Column(name = "evaluation_specificity")
    private Integer evaluationSpecificity;

    @Column(name = "evaluation_practicality")
    private Integer evaluationPracticality;

    @Column(name = "evaluation_validity")
    private Integer evaluationValidity;

    @Column(name = "evaluation_total_score")
    private Integer evaluationTotalScore;

    @Column(name = "evaluation_feedback", columnDefinition = "TEXT")
    private String evaluationFeedback;
}
