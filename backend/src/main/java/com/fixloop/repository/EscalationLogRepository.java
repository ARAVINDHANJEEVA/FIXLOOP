package com.fixloop.repository;

import com.fixloop.domain.EscalationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EscalationLogRepository extends JpaRepository<EscalationLog, Integer> {
    List<EscalationLog> findByComplaintIdOrderByTimestampDesc(Integer complaintId);
}
