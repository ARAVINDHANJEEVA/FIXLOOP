package com.fixloop.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlaTrendDto {
    private String period;
    private Long breached;
    private Long resolved;
    private Long atRisk;
}
