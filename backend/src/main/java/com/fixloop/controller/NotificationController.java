package com.fixloop.controller;

import com.fixloop.domain.AppUser;
import com.fixloop.dto.notification.NotificationDto;
import com.fixloop.repository.AppUserRepository;
import com.fixloop.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final AppUserRepository userRepo;

    private AppUser getCurrentUser(Authentication auth) {
        return userRepo.findByEmail(auth.getName()).orElseThrow();
    }

    /**
     * GET /api/notifications?filter=all|unread|action_needed&page=&size=
     * Returns paginated list of notifications for the authenticated user.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT','STAFF','ADMIN')")
    public ResponseEntity<Page<NotificationDto>> getNotifications(
            @RequestParam(defaultValue = "all") String filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        AppUser user = getCurrentUser(auth);
        return ResponseEntity.ok(notificationService.getNotifications(user.getId(), filter, page, size));
    }

    /**
     * PATCH /api/notifications/{id}/read — Mark a single notification as read
     */
    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('STUDENT','STAFF','ADMIN')")
    public ResponseEntity<Map<String, String>> markRead(
            @PathVariable Integer id,
            Authentication auth) {
        AppUser user = getCurrentUser(auth);
        notificationService.markRead(id, user.getId());
        return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
    }

    /**
     * PATCH /api/notifications/read-all — Mark all notifications as read
     */
    @PatchMapping("/read-all")
    @PreAuthorize("hasAnyRole('STUDENT','STAFF','ADMIN')")
    public ResponseEntity<Map<String, Object>> markAllRead(Authentication auth) {
        AppUser user = getCurrentUser(auth);
        notificationService.markAllRead(user.getId());
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }
}
