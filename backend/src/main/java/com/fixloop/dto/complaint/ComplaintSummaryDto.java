package com.fixloop.dto.complaint;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintSummaryDto {
    private Integer id;
    private String token;
    private Integer categoryId;
    private String categoryName;
    private Integer locationId;
    private LocationDto location;
    private String description;
    private Short severity;
    private Integer studentsAffected;
    private String status;
    private BigDecimal priorityScore;
    private boolean isAnonymous;
    private Integer incidentId;
    private Integer incidentCount;
    private Integer recurrenceCountLast90Days;
    private Integer assignedToStaffId;
    private String dispatchType;
    private Short escalationLevel;
    private boolean isAtRisk;
    private boolean isBreached;
    private String createdAt;
    private String slaDeadline;
    private String resolvedAt;
    private List<String> siblingTokens;

    // ── Clustering fields (new) ──
    /** IDs of all sibling complaints in the same incident (for UI comma display). */
    private List<Integer> siblingIds;
    /** True if an admin manually removed this complaint from a cluster. */
    private boolean clusterExcluded;
    /** The clustering confidence score (0.0–1.0) computed at clustering time. */
    private BigDecimal clusterScore;
}
