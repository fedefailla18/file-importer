package com.importer.fileimporter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.importer.fileimporter.entity.ExternalApiRawResponse;
import com.importer.fileimporter.entity.ExchangeName;
import com.importer.fileimporter.entity.User;
import com.importer.fileimporter.repository.ExternalApiRawResponseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class RawResponseService {

    private final ExternalApiRawResponseRepository repository;
    private final ObjectMapper objectMapper;

    public void saveResponse(User user, ExchangeName exchange, String type, String externalId, Object body) {
        try {
            ExternalApiRawResponse rawResponse = ExternalApiRawResponse.builder()
                    .user(user)
                    .exchangeName(exchange)
                    .responseType(type)
                    .externalId(externalId)
                    .rawJson(objectMapper.writeValueAsString(body))
                    .fetchedAt(LocalDateTime.now())
                    .build();
            repository.save(rawResponse);
        } catch (Exception e) {
            log.error("Failed to save raw API response for {} {}: {}", exchange, type, e.getMessage());
        }
    }
}
