package com.importer.fileimporter.service;

import com.importer.fileimporter.dto.integration.iol.*;
import com.importer.fileimporter.entity.ExchangeName;
import com.importer.fileimporter.entity.User;
import com.importer.fileimporter.entity.UserExchangeConfig;
import com.importer.fileimporter.repository.UserExchangeConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
@Slf4j
public class IolApiService {

    private static final String BASE_URL = "https://api.invertironline.com";
    private static final String TOKEN_ENDPOINT = "/token";
    private static final String ACCOUNT_STATEMENT_ENDPOINT = "/api/v2/estadocuenta";
    private static final String PORTFOLIO_ENDPOINT = "/api/v2/portafolio/{pais}";
    private static final String OPERATIONS_ENDPOINT = "/api/v2/operaciones";
    private static final String PROFILE_ENDPOINT = "/api/v2/datos-perfil";

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

    public IolAccountStatementResponse getAccountStatement(User user) {
        String token = loginForUser(user).getAccessToken(); // Simple strategy: login per request for now
        return webClient.get()
                .uri(ACCOUNT_STATEMENT_ENDPOINT)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(IolAccountStatementResponse.class)
                .block();
    }

    public IolPortfolioResponse getPortfolio(User user, String country) {
        String token = loginForUser(user).getAccessToken();
        return webClient.get()
                .uri(PORTFOLIO_ENDPOINT, country)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(IolPortfolioResponse.class)
                .block();
    }

    public List<IolOperationResponse> getOperations(User user) {
        String token = loginForUser(user).getAccessToken();
        return webClient.get()
                .uri(OPERATIONS_ENDPOINT)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<IolOperationResponse>>() {})
                .block();
    }

    public IolOperationResponse getOperationDetails(User user, Long operationNumber) {
        String token = loginForUser(user).getAccessToken();
        return webClient.get()
                .uri(OPERATIONS_ENDPOINT + "/" + operationNumber)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(IolOperationResponse.class)
                .block();
    }

    public IolProfileResponse getProfile(User user) {
        String token = loginForUser(user).getAccessToken();
        return webClient.get()
                .uri(PROFILE_ENDPOINT)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(IolProfileResponse.class)
                .block();
    }
}
