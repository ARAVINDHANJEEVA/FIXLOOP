package com.fixloop.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "complaint")
public class Complaint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_user_id")
    private StudentProfile studentUser;

    @Column(name = "is_anonymous")
    private boolean isAnonymous;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    private Incident incident;

    private String description;

    private Short severity;

    @Column(name = "students_affected")
    private Integer studentsAffected;

    @Column(name = "recurrence_count_90d")
    private Integer recurrenceCount90d;

    @Builder.Default
    private String status = "SUBMITTED";

    @Column(name = "priority_score")
    private BigDecimal priorityScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extra_data", columnDefinition = "jsonb")
    private String extraData;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by_admin_id")
    private AdminProfile assignedByAdmin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_staff_id")
    private StaffProfile assignedToStaff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routed_department_id")
    private Department routedDepartment;

    @Column(name = "dispatch_type")
    private String dispatchType;

    @Column(name = "escalation_level")
    private Short escalationLevel;

    @Column(name = "is_at_risk")
    private boolean isAtRisk;

    @Column(name = "is_breached")
    private boolean isBreached;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "sla_deadline")
    private LocalDateTime slaDeadline;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "cluster_excluded")
    private boolean clusterExcluded;

    @Column(name = "cluster_score")
    private BigDecimal clusterScore;
}
