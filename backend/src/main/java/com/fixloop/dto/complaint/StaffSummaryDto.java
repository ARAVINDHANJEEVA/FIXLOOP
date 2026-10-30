package com.fixloop.dto.complaint;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffSummaryDto {
    private Integer id;
    private String name;
    private String jobTitle;
    private Integer departmentId;
    private String departmentName;
    private BigDecimal currentWeightedLoad;
    private String avatarInitials;
    private String email;
}
