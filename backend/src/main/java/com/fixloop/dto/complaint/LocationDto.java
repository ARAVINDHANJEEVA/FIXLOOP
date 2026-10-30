package com.fixloop.dto.complaint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationDto {
    private Integer id;
    private String building;
    private String floor;
    private String room;
    private String campusZone;
}
