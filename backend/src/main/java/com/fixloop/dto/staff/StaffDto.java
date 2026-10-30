package com.fixloop.dto.staff;

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
public class StaffDto {
    private Integer id;
    private String name;
    private String email;
    private String jobTitle;
    private Integer departmentId;
    private String departmentName;
    private List<String> skillTags;
    private String status;
    private BigDecimal currentWeightedLoad;
    private String avatarInitials;
}
