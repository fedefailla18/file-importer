package com.importer.fileimporter.service;

import com.importer.fileimporter.dto.integration.iol.IolTokenResponse;
import com.importer.fileimporter.entity.ExchangeName;
import com.importer.fileimporter.entity.User;
import com.importer.fileimporter.entity.UserExchangeConfig;
import com.importer.fileimporter.repository.UserExchangeConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
public class IolApiService {

    private static final String BASE_URL = "https://api.invertironline.com";
    private static final String TOKEN_ENDPOINT = "/token";

    private final WebClient webClient;
    private final UserExchangeConfigRepository userExchangeConfigRepository;
    private final EncryptionService encryptionService;

    public IolApiService(WebClient.Builder webClientBuilder, 
                         UserExchangeConfigRepository userExchangeConfigRepository, 
                         EncryptionService encryptionService) {
        this.webClient = webClientBuilder.baseUrl(BASE_URL).build();
        this.userExchangeConfigRepository = userExchangeConfigRepository;
        this.encryptionService = encryptionService;
    }

    public IolTokenResponse login(String username, String password) {
        log.info("Attempting IOL login for user: {}", username);
        
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("username", username);
        formData.add("password", password);
        formData.add("grant_type", "password");

        return webClient.post()
                .uri(TOKEN_ENDPOINT)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(IolTokenResponse.class)
                .block();
    }

    public IolTokenResponse loginForUser(User user) {
        UserExchangeConfig config = userExchangeConfigRepository.findByUserAndExchangeName(user, ExchangeName.IOL)
                .orElseThrow(() -> new IllegalArgumentException("IOL credentials not configured for user"));

        String username = config.getApiKey();
        String password = encryptionService.decrypt(config.getApiSecret());

        return login(username, password);
    }

    public IolTokenResponse refreshToken(String refreshToken) {
        log.info("Attempting IOL token refresh");
        
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("refresh_token", refreshToken);
        formData.add("grant_type", "refresh_token");

        return webClient.post()
                .uri(TOKEN_ENDPOINT)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(IolTokenResponse.class)
                .block();
    }
}
