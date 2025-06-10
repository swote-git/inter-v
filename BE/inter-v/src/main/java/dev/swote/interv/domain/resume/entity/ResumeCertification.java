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
@Table(name = "tb_resume_certification")
public class ResumeCertification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @JoinColumn(name = "resume_id")
    private Resume resume;

    private String certificationName;

    private String issuingOrganization;

    private LocalDate acquiredDate;

    private LocalDate expiryDate;

    private Boolean noExpiry;
}