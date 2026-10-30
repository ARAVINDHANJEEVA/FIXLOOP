package com.fixloop.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffPerformanceDto {
    private Integer staffId;
    private String staffName;
    private String department;
    private Double avgResolutionHours;
    private Double slaCompliancePercent;
    private Integer openTickets;
    private Integer resolvedThisMonth;
}
