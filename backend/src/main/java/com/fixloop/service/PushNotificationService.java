package com.fixloop.service;

import com.fixloop.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Sends FCM data-only push notifications to all registered devices of a user.
 *
 * FCM is opt-in: set fixloop.fcm.enabled=true and provide a valid
 * fixloop.fcm.credentials-path (Firebase service account JSON) to activate.
 * When disabled, this service is a no-op and all other flows continue normally.
 *
 * Data-only messages (no "notification" block) are used intentionally so that
 * the Android app controls whether to show a system tray notification:
 *   - Foregrounded: handled by WebSocket; no tray popup shown.
 *   - Backgrounded: FirebaseMessagingService shows a local notification.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

    @Value("${fixloop.fcm.enabled:false}")
    private boolean fcmEnabled;

    @Value("${fixloop.fcm.credentials-path:}")
    private String credentialsPath;

    private final DeviceTokenRepository deviceTokenRepo;

    /**
     * Send a data-only FCM message to all devices registered for this user.
     * All values in the data map must be Strings (FCM requirement).
     */
    public void send(Integer userId, Map<String, String> data) {
        if (!fcmEnabled) {
            log.debug("FCM disabled — skipping push for userId={}", userId);
            return;
        }

        var tokens = deviceTokenRepo.findByUserId(userId);
        if (tokens.isEmpty()) {
            log.debug("No FCM tokens registered for userId={}", userId);
            return;
        }

        /*
         * ── Firebase Admin SDK integration ──────────────────────────────────
         * To activate, add to pom.xml:
         *   <dependency>
         *     <groupId>com.google.firebase</groupId>
         *     <artifactId>firebase-admin</artifactId>
         *     <version>9.3.0</version>
         *   </dependency>
         *
         * Then uncomment the block below and set fixloop.fcm.enabled=true
         * with a valid credentials-path pointing to your service account JSON.
         *
         * ── Uncomment when Firebase Admin SDK is on classpath ───────────────
         *
         * for (var deviceToken : tokens) {
         *     try {
         *         Message message = Message.builder()
         *             .setToken(deviceToken.getFcmToken())
         *             .putAllData(data)
         *             .build();
         *         String response = FirebaseMessaging.getInstance().send(message);
         *         log.debug("FCM sent to token={} response={}", deviceToken.getFcmToken(), response);
         *     } catch (FirebaseMessagingException e) {
         *         log.warn("FCM send failed for token={}: {}", deviceToken.getFcmToken(), e.getMessage());
         *         if ("registration-token-not-registered".equals(e.getErrorCode().name())) {
         *             deviceTokenRepo.deleteByFcmToken(deviceToken.getFcmToken());
         *             log.info("Removed stale FCM token={}", deviceToken.getFcmToken());
         *         }
         *     }
         * }
         */

        log.info("FCM enabled but Firebase Admin SDK not yet initialized. " +
                 "Add firebase-admin to pom.xml and configure credentials-path.");
    }
}
