package dev.swote.interv.domain.interview.entity;

import dev.swote.interv.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "answer_evaluations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerEvaluation extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "answer_id")
    private Answer answer;

    @Column(name = "relevance")
    private Integer relevance;

    @Column(name = "specificity")
    private Integer specificity;

    @Column(name = "practicality")
    private Integer practicality;

    @Column(name = "validity")
    private Integer validity;

    @Column(name = "total_score")
    private Integer totalScore;

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "evaluation_type")
    private String evaluationType; // "AI", "HUMAN", "AUTO"
}
