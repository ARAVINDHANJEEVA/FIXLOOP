package com.fixloop.dto.complaint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * A single complaint's data as a member of a cluster (incident).
 * Used in ClusterPanel UI to display comma-separated IDs and descriptions.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClusterMemberDto {
    private Integer id;
    private String token;
    private String description;
    private String status;
    private BigDecimal clusterScore;
    private String createdAt;
}
