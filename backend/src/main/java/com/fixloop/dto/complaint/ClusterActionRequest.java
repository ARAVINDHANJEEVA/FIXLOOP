package com.fixloop.dto.complaint;

import lombok.Data;

/**
 * Request body for POST /api/complaints/{id}/cluster and /api/complaints/{id}/uncluster.
 */
@Data
public class ClusterActionRequest {
    /** Target incident ID for manual cluster. Null for uncluster. */
    private Integer targetIncidentId;
    /** Reason / audit note (required for uncluster, optional for cluster). */
    private String reason;
}
