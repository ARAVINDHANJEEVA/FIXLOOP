package com.fixloop.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Embeddable
public class ComplaintVendorId implements Serializable {
    @Column(name = "complaint_id")
    private Integer complaintId;

    @Column(name = "vendor_id")
    private Integer vendorId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComplaintVendorId that = (ComplaintVendorId) o;
        return Objects.equals(complaintId, that.complaintId) &&
               Objects.equals(vendorId, that.vendorId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(complaintId, vendorId);
    }
}
