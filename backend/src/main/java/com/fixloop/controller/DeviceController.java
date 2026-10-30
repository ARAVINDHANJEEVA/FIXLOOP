package com.fixloop.controller;

import com.fixloop.domain.AppUser;
import com.fixloop.dto.notification.DeviceRegisterRequest;
import com.fixloop.repository.AppUserRepository;
import com.fixloop.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final NotificationService notificationService;
    private final AppUserRepository userRepo;

    private AppUser getCurrentUser(Authentication auth) {
        return userRepo.findByEmail(auth.getName()).orElseThrow();
    }

    /**
     * POST /api/devices/register
     * Called after login to register an FCM token for push notifications.
     * Upserts: if token already exists, updates timestamp.
     */
    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('STUDENT','STAFF','ADMIN')")
    public ResponseEntity<Map<String, String>> register(
            @Valid @RequestBody DeviceRegisterRequest request,
            Authentication auth) {
        AppUser user = getCurrentUser(auth);
        notificationService.registerDevice(user.getId(), request);
        return ResponseEntity.ok(Map.of("message", "Device registered successfully"));
    }

    /**
     * DELETE /api/devices/{fcmToken}
     * Called on logout to deregister the FCM token so push stops.
     */
    @DeleteMapping("/{fcmToken}")
    @PreAuthorize("hasAnyRole('STUDENT','STAFF','ADMIN')")
    public ResponseEntity<Map<String, String>> unregister(
            @PathVariable String fcmToken,
            Authentication auth) {
        notificationService.unregisterDevice(fcmToken);
        return ResponseEntity.ok(Map.of("message", "Device unregistered successfully"));
    }
}
