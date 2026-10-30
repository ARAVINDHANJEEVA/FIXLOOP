package com.fixloop.repository;

import com.fixloop.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    Page<Notification> findByRecipientUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    Page<Notification> findByRecipientUserIdAndReadFalseOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    Page<Notification> findByRecipientUserIdAndTypeInOrderByCreatedAtDesc(
            Integer userId, List<String> types, Pageable pageable);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.recipientUser.id = :userId AND n.read = false")
    int markAllReadForUser(@Param("userId") Integer userId);
}
