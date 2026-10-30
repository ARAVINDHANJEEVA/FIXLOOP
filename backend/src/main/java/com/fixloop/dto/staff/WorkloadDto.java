package com.fixloop.dto.staff;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkloadDto {
    private Integer userId;
    private String name;
    private Integer openTickets;
    private BigDecimal currentWeightedLoad;
    private Integer resolvedThisWeek;
    private Double avgResolutionHours;
}
