package com.fixloop.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "priority_weight_config")
public class PriorityWeightConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "w1_severity")
    private BigDecimal w1Severity;

    @Column(name = "w2_students")
    private BigDecimal w2Students;

    @Column(name = "w3_recurrence")
    private BigDecimal w3Recurrence;

    @Column(name = "w4_time_decay")
    private BigDecimal w4TimeDecay;

    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;
}
