package com.fixloop.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto {
    private Integer id;
    private String name;
    private String icon;
    private String color;
    private boolean sensitive;
    private BigDecimal baseSeverityWeight;
    private Integer defaultSlaHours;
    private Integer departmentId;
    private String formSchema;
    private List<Integer> requiredSkillIds;
    private List<String> requiredSkillNames;
    /** Mobile filter-pill grouping: FACILITIES | ACADEMIC_TECH | WELFARE_MISSING | null (sensitive) */
    private String filterGroup;
}
