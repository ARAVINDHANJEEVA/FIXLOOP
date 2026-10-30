package com.fixloop.dto.staff;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffUpdateRequest {
    private String name;
    private String jobTitle;
    private Integer departmentId;
    private List<Integer> skillIds;
    private String status;
}
