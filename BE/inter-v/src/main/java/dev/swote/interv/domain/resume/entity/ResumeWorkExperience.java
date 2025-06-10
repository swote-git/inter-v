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
@Table(name = "tb_resume_work_experience")
public class ResumeWorkExperience extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id")
    private Resume resume;

    private String companyName;
    private String position;
    private String department;
    private String location;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean currentlyWorking;

    @Column(columnDefinition = "TEXT")
    private String responsibilities;

    @Column(columnDefinition = "TEXT")
    private String achievements;
}