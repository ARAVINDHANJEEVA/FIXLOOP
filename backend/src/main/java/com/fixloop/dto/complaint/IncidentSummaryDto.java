package com.fixloop.dto.complaint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Summary of an incident (complaint cluster) with all member complaints.
 * Returned inside ComplaintDetailDto when a complaint is part of a cluster.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentSummaryDto {
    private Integer id;
    private String status;
    private Integer complaintCount;
    /** First complaint's description — display-only summary for the incident card. */
    private String summary;
    private List<ClusterMemberDto> members;
    private String createdAt;
    private String lastComplaintAt;
}
