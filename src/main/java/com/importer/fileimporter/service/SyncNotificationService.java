package com.importer.fileimporter.service;

import com.importer.fileimporter.dto.SyncStatusMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyCompleted(String username, String portfolioName) {
        send(username, new SyncStatusMessage(portfolioName, "COMPLETED",
                "Full historical sync completed for portfolio: " + portfolioName));
    }

    public void notifyFailed(String username, String portfolioName, String error) {
        send(username, new SyncStatusMessage(portfolioName, "FAILED",
                error != null ? error : "Unknown error"));
    }

    private void send(String username, SyncStatusMessage msg) {
        try {
            messagingTemplate.convertAndSendToUser(username, "/queue/sync-status", msg);
            log.info("Sent sync status [{}] to user {}", msg.getStatus(), username);
        } catch (Exception e) {
            log.warn("Failed to push WebSocket notification to {}: {}", username, e.getMessage());
        }
    }
}
