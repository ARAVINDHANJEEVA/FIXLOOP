package com.fixloop.repository;

import com.fixloop.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Integer> {
    Optional<Review> findByComplaintId(Integer complaintId);
    boolean existsByComplaintId(Integer complaintId);
}
