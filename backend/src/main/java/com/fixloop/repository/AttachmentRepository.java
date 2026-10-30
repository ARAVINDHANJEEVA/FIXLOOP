package com.fixloop.repository;

import com.fixloop.domain.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Integer> {
    List<Attachment> findByComplaintId(Integer complaintId);
}
