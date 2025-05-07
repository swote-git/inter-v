package dev.swote.interv.domain.position.entity;

import dev.swote.interv.domain.BaseEntity;
import dev.swote.interv.domain.company.entity.Company;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tb_position")
public class Position extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ElementCollection
    @CollectionTable(name = "position_skills", joinColumns = @JoinColumn(name = "position_id"))
    @Column(name = "skill")
    private Set<String> requiredSkills = new HashSet<>();
}