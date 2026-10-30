package com.fixloop.dto.student;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfileDto {
    private Integer userId;
    private String name;
    private String email;
    private String rollNo;
    private String avatarInitials;
    private Integer departmentId;
    private String departmentName;
    private Integer defaultLocationId;
    private String defaultLocationBuilding;
    private String defaultLocationRoom;
    private String createdAt;
}
