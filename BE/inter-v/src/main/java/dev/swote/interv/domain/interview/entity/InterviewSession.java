package dev.swote.interv.domain.interview.entity;

import dev.swote.interv.domain.BaseEntity;
import dev.swote.interv.domain.position.entity.Position;
import dev.swote.interv.domain.resume.entity.Resume;
import dev.swote.interv.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tb_interview_session")
public class InterviewSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id")
    private Resume resume;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id")
    private Position position;

    @Enumerated(EnumType.STRING)
    private InterviewType type;

    @Enumerated(EnumType.STRING)
    private InterviewStatus status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String shareUrl;

    @OneToMany(mappedBy = "interviewSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question> questions = new ArrayList<>();
}