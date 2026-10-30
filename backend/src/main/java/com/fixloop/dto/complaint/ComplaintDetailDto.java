package com.fixloop.dto.complaint;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintDetailDto extends ComplaintSummaryDto {
    private StudentDto student;
    private StaffSummaryDto assignedToStaff;
    private ComplaintVendorDto vendorAssignment;
    private List<StatusHistoryEntryDto> statusHistory;
    private List<AttachmentDto> attachments;
    private ReviewDto review;
    private String extraData;
    /** Full cluster detail with all sibling complaints. Null if not clustered. */
    private IncidentSummaryDto incident;
}
