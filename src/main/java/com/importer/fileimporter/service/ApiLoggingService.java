package com.importer.fileimporter.service;

import com.importer.fileimporter.config.security.services.CurrentUserProvider;
import com.importer.fileimporter.entity.ExternalApiLog;
import com.importer.fileimporter.entity.User;
import com.importer.fileimporter.repository.ExternalApiLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApiLoggingService {

    private final ExternalApiLogRepository externalApiLogRepository;
    private final CurrentUserProvider currentUserProvider;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String provider, String endpoint, String method, String params, Integer status, String responseBody, Integer weight) {
        try {
            User currentUser = currentUserProvider.getCurrentUser();
            
            ExternalApiLog apiLog = ExternalApiLog.builder()
                    .timestamp(LocalDateTime.now())
                    .provider(provider)
                    .endpoint(endpoint)
                    .method(method)
                    .requestParams(params)
                    .responseStatus(status)
                    .responseBody(truncate(responseBody, 10000)) // Avoid too large bodies
                    .usedWeight(weight)
                    .user(currentUser)
                    .build();

            externalApiLogRepository.save(apiLog);
        } catch (Exception e) {
            log.error("Failed to save API log: {}", e.getMessage());
        }
    }

    private String truncate(String text, int length) {
        if (text == null || text.length() <= length) {
            return text;
        }
        return text.substring(0, length) + "... [TRUNCATED]";
    }
}
