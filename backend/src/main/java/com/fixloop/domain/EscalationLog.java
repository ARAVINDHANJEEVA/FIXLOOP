package com.fixloop.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "escalation_log")
public class EscalationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_id")
    private Complaint complaint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_staff_id")
    private StaffProfile fromStaff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_staff_id")
    private StaffProfile toStaff;

    @Column(name = "previous_level")
    private Short previousLevel;

    @Column(name = "new_level")
    private Short newLevel;

    private String reason;

    @Column(name = "triggered_by")
    private String triggeredBy;

    @CreationTimestamp
    @Column(name = "timestamp", updatable = false)
    private LocalDateTime timestamp;
}
