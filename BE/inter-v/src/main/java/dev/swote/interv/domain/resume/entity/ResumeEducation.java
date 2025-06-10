package dev.swote.interv.domain.resume.entity;

import dev.swote.interv.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tb_resume_education")
public class ResumeEducation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id")
    private Resume resume;

    private String schoolType;
    private String schoolName;
    private String location;
    private String major;
    private LocalDate enrollmentDate;
    private LocalDate graduationDate;
    private Boolean inProgress;
    private String gpa;
}