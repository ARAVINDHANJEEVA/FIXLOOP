package com.fixloop.dto.complaint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusHistoryEntryDto {
    private Integer id;
    private String status;
    private String changedBy;
    private String changedByRole;
    private String note;
    private String timestamp;
}
