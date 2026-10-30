package com.fixloop.service;

import com.fixloop.domain.*;
import com.fixloop.dto.notification.DeviceRegisterRequest;
import com.fixloop.dto.notification.NotificationDto;
import com.fixloop.repository.AppUserRepository;
import com.fixloop.repository.DeviceTokenRepository;
import com.fixloop.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepo;
    private final DeviceTokenRepository deviceTokenRepo;
    private final AppUserRepository userRepo;
    private final PushNotificationService pushNotificationService;

    // ── WebSocket broadcasts ────────────────────────────────────────

    /** New complaint submitted — admin queue update */
    public void broadcastNewComplaint(Object dto) {
        messagingTemplate.convertAndSend("/topic/admin/queue", dto);
    }

    /** Status changed on a specific complaint — all subscribers */
    public void broadcastStatusChange(Integer complaintId, String newStatus) {
        Map<String, Object> payload = Map.of(
            "complaintId", complaintId,
            "status", newStatus,
            "timestamp", LocalDateTime.now().toString()
        );
        messagingTemplate.convertAndSend("/topic/complaint/" + complaintId, payload);
    }

    /** Universal complaint update broadcast — sent to /topic/complaints for real-time web sync */
    public void broadcastComplaintUpdate(Object dto) {
        messagingTemplate.convertAndSend("/topic/complaints", dto);
    }

    /** Assignment notification to a specific staff member's queue */
    public void broadcastAssignment(Integer staffUserId, Object dto) {
        messagingTemplate.convertAndSend("/topic/staff/" + staffUserId + "/queue", dto);
    }

    /** SLA at-risk or breach event — admin SLA dashboard */
    public void broadcastSlaAlert(Map<String, Object> alert) {
        messagingTemplate.convertAndSend("/topic/sla/alerts", alert);
    }

    /** Admin alert when auto-assignment fails or manual intervention needed */
    public void broadcastAdminAlert(Map<String, Object> alert) {
        messagingTemplate.convertAndSend("/topic/admin/alerts", alert);
    }

    /**
     * Student personal topic — delivers real-time status updates while app is foregrounded.
     * Shape matches the FCM data-only payload (§7 of integration spec).
     */
    public void broadcastStudentUpdate(Integer studentUserId, Map<String, Object> payload) {
        messagingTemplate.convertAndSend("/topic/student/" + studentUserId, payload);
    }

    // ── Fan-out point: DB row + WS + FCM ───────────────────────────

    /**
     * Central fan-out method. Called whenever a state change affecting a student occurs.
     * Writes notification row, sends WS to student topic, triggers FCM push.
     */
    public void persistAndBroadcast(AppUser recipient, Complaint complaint,
                                     String type, String title, String body) {
        // 1. Persist notification row
        Notification notification = Notification.builder()
            .recipientUser(recipient)
            .complaint(complaint)
            .type(type)
            .title(title)
            .body(body)
            .statusAtSend(complaint.getStatus())
            .read(false)
            .build();
        notificationRepo.save(notification);

        // 2. WS broadcast to student topic (foregrounded app)
        Map<String, Object> payload = Map.of(
            "type", type,
            "complaintId", complaint.getId(),
            "token", complaint.getToken() != null ? complaint.getToken() : "",
            "newStatus", complaint.getStatus(),
            "title", title,
            "body", body,
            "timestamp", LocalDateTime.now().toString()
        );
        broadcastStudentUpdate(recipient.getId(), payload);

        // 3. FCM data-only push (backgrounded app) — no-op if disabled
        Map<String, String> fcmData = Map.of(
            "type", type,
            "complaintId", String.valueOf(complaint.getId()),
            "token", complaint.getToken() != null ? complaint.getToken() : "",
            "newStatus", complaint.getStatus(),
            "title", title,
            "body", body
        );
        pushNotificationService.send(recipient.getId(), fcmData);
    }

    // ── Notification CRUD ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<NotificationDto> getNotifications(Integer userId, String filter, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> results;
        switch (filter != null ? filter.toLowerCase() : "all") {
            case "unread" -> results = notificationRepo
                .findByRecipientUserIdAndReadFalseOrderByCreatedAtDesc(userId, pageable);
            case "action_needed" -> results = notificationRepo
                .findByRecipientUserIdAndTypeInOrderByCreatedAtDesc(
                    userId, List.of("AWAITING_INFO"), pageable);
            default -> results = notificationRepo
                .findByRecipientUserIdOrderByCreatedAtDesc(userId, pageable);
        }
        return results.map(this::toDto);
    }

    public void markRead(Integer notificationId, Integer userId) {
        Notification n = notificationRepo.findById(notificationId)
            .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));
        if (!n.getRecipientUser().getId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Not your notification");
        }
        n.setRead(true);
        notificationRepo.save(n);
    }

    public void markAllRead(Integer userId) {
        notificationRepo.markAllReadForUser(userId);
    }

    // ── Device token management ─────────────────────────────────────

    public void registerDevice(Integer userId, DeviceRegisterRequest request) {
        AppUser user = userRepo.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        deviceTokenRepo.findByFcmToken(request.getFcmToken()).ifPresentOrElse(
            existing -> {
                existing.setUpdatedAt(LocalDateTime.now());
                deviceTokenRepo.save(existing);
            },
            () -> {
                DeviceToken token = DeviceToken.builder()
                    .user(user)
                    .fcmToken(request.getFcmToken())
                    .platform(request.getPlatform() != null ? request.getPlatform() : "ANDROID")
                    .updatedAt(LocalDateTime.now())
                    .build();
                deviceTokenRepo.save(token);
            }
        );
    }

    public void unregisterDevice(String fcmToken) {
        deviceTokenRepo.deleteByFcmToken(fcmToken);
    }

    // ── DTO mapping ─────────────────────────────────────────────────

    private NotificationDto toDto(Notification n) {
        return NotificationDto.builder()
            .id(n.getId())
            .complaintId(n.getComplaint() != null ? n.getComplaint().getId() : null)
            .complaintToken(n.getComplaint() != null ? n.getComplaint().getToken() : null)
            .type(n.getType())
            .title(n.getTitle())
            .body(n.getBody())
            .statusAtSend(n.getStatusAtSend())
            .read(n.isRead())
            .createdAt(n.getCreatedAt() != null ? n.getCreatedAt().toString() : null)
            .build();
    }
}
