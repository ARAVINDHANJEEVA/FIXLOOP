package com.fixloop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlaRuleDto {
    private Integer id;
    private Integer categoryId;
    private String categoryName;
    private String severityTier;
    private Integer resolutionHours;
    private Integer escalationHours;
}
