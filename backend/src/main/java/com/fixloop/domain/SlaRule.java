package com.fixloop.domain;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "sla_rule", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"category_id", "severity_tier"})
})
public class SlaRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "severity_tier")
    private String severityTier;

    @Column(name = "resolution_hours")
    private Integer resolutionHours;

    @Column(name = "escalation_hours")
    private Integer escalationHours;
}
