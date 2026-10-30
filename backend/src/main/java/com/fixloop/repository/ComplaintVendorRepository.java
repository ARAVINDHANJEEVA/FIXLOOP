package com.fixloop.repository;

import com.fixloop.domain.ComplaintVendor;
import com.fixloop.domain.ComplaintVendorId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ComplaintVendorRepository extends JpaRepository<ComplaintVendor, ComplaintVendorId> {
    List<ComplaintVendor> findByComplaintId(Integer complaintId);
    Optional<ComplaintVendor> findByComplaintIdAndVendorId(Integer complaintId, Integer vendorId);
}
