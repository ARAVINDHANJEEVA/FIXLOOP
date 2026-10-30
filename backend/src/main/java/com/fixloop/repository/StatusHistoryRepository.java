package com.fixloop.repository;

import com.fixloop.domain.StatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Integer> {
    List<StatusHistory> findByComplaintIdOrderByTimestampAsc(Integer complaintId);
}
