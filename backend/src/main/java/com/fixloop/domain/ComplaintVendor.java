package com.fixloop.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "complaint_vendor")
@IdClass(ComplaintVendorId.class)
public class ComplaintVendor {
    @Id
    @Column(name = "complaint_id")
    private Integer complaintId;

    @Id
    @Column(name = "vendor_id")
    private Integer vendorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_id", insertable = false, updatable = false)
    private Complaint complaint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", insertable = false, updatable = false)
    private Vendor vendor;

    @Builder.Default
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "contact_status")
    private String contactStatus = "CONTACTED";

    private String notes;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
