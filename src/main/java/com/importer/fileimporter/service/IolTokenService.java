package com.importer.fileimporter.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.importer.fileimporter.client.IolClient;
import com.importer.fileimporter.dto.integration.iol.IolTokenResponse;
import com.importer.fileimporter.entity.ExchangeName;
import com.importer.fileimporter.entity.User;
import com.importer.fileimporter.entity.UserExchangeConfig;
import com.importer.fileimporter.repository.UserExchangeConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class IolTokenService {

    private final IolClient iolClient;
    private final UserExchangeConfigRepository userExchangeConfigRepository;
    private final EncryptionService encryptionService;

    // Cache tokens for 14 minutes (IOL tokens expire in 15)
    private final Cache<UUID, IolTokenResponse> tokenCache = Caffeine.newBuilder()
            .expireAfterWrite(14, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    public IolTokenService(IolClient iolClient, 
                           UserExchangeConfigRepository userExchangeConfigRepository, 
                           EncryptionService encryptionService) {
        this.iolClient = iolClient;
        this.userExchangeConfigRepository = userExchangeConfigRepository;
        this.encryptionService = encryptionService;
    }

    public String getAccessToken(User user) {
        IolTokenResponse tokenResponse = tokenCache.get(user.getId(), k -> fetchNewToken(user));
        return tokenResponse != null ? tokenResponse.getAccessToken() : null;
    }

    public IolTokenResponse getAccessTokenResponse(User user) {
        return tokenCache.get(user.getId(), k -> fetchNewToken(user));
    }

    private IolTokenResponse fetchNewToken(User user) {
        UserExchangeConfig config = userExchangeConfigRepository.findByUserAndExchangeName(user, ExchangeName.IOL)
                .orElseThrow(() -> new IllegalArgumentException("IOL credentials not configured for user"));

        String username = config.getApiKey();
        String password = encryptionService.decrypt(config.getApiSecret());

        log.info("Fetching new IOL token for user: {}", user.getUsername());

        Map<String, String> formData = new HashMap<>();
        formData.put("username", username);
        formData.put("password", password);
        formData.put("grant_type", "password");

        return iolClient.login(formData);
    }

    public void evictToken(User user) {
        tokenCache.invalidate(user.getId());
    }
}
