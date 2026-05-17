package com.fixall.backend.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Sends push notifications via Firebase Cloud Messaging (FCM).
 * Requires a Firebase service account JSON at src/main/resources/firebase-service-account.json.
 * If the file is missing, the service operates in stub mode (logs instead of sending).
 */
@Service
@Slf4j
public class NotificationService {

    private boolean firebaseInitialized = false;

    @PostConstruct
    public void init() {
        try {
            InputStream serviceAccount = getClass().getClassLoader()
                .getResourceAsStream("firebase-service-account.json");

            if (serviceAccount == null) {
                // Try filesystem path (for when the file is outside resources)
                try {
                    serviceAccount = new FileInputStream("firebase-service-account.json");
                } catch (IOException ignored) {
                    // File not found
                }
            }

            if (serviceAccount == null) {
                log.warn("⚠ Firebase service account JSON not found. " +
                         "Push notifications will be logged but NOT sent. " +
                         "Add firebase-service-account.json to src/main/resources/ to enable FCM.");
                return;
            }

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
                FirebaseApp.initializeApp(options);
                firebaseInitialized = true;
                log.info("✅ Firebase initialized successfully — push notifications enabled");
            } else {
                firebaseInitialized = true;
            }
        } catch (IOException e) {
            log.error("Failed to initialize Firebase", e);
        }
    }

    /**
     * Send a push notification to a specific device.
     *
     * @param fcmToken the device's FCM registration token
     * @param title    notification title
     * @param body     notification body text
     * @param data     optional key-value data payload (for handling in-app)
     */
    public void sendPush(String fcmToken, String title, String body, Map<String, String> data) {
        if (fcmToken == null || fcmToken.isBlank()) {
            log.debug("No FCM token provided, skipping notification: {}", title);
            return;
        }

        if (!firebaseInitialized) {
            log.info("[STUB NOTIFICATION] To: {} | Title: {} | Body: {} | Data: {}", 
                     fcmToken.substring(0, Math.min(10, fcmToken.length())) + "...", 
                     title, body, data);
            return;
        }

        try {
            Message.Builder messageBuilder = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build());

            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            String messageId = FirebaseMessaging.getInstance().send(messageBuilder.build());
            log.info("FCM notification sent: {} -> {}", messageId, title);

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM notification to token: {}", 
                      fcmToken.substring(0, Math.min(10, fcmToken.length())), e);
        }
    }

    /**
     * Convenience: send notification about a job status change.
     */
    public void notifyJobStatusChange(String fcmToken, String jobId, String jobTitle, String newStatus, String message) {
        sendPush(fcmToken, "FixAll — " + newStatus, message, Map.of(
            "type", "JOB_STATUS_CHANGE",
            "jobId", jobId,
            "jobTitle", jobTitle,
            "newStatus", newStatus
        ));
    }
}
