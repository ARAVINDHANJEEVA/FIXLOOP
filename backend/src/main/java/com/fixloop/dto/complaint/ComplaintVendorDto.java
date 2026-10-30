package com.fixloop.dto.complaint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintVendorDto {
    private Integer vendorId;
    private String vendorName;
    private String assignedAt;
    private String contactStatus;
    private String notes;
}
