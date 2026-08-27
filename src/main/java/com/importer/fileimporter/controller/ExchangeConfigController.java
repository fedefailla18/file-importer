package com.importer.fileimporter.controller;

import com.importer.fileimporter.entity.User;
import com.importer.fileimporter.payload.response.BinanceSpotActivityResponse;
import com.importer.fileimporter.payload.response.MexcSpotActivityResponse;
import com.importer.fileimporter.entity.UserExchangeConfig;
import com.importer.fileimporter.payload.request.ExchangeConfigRequest;
import com.importer.fileimporter.payload.response.ExchangeConfigResponse;
import com.importer.fileimporter.repository.UserExchangeConfigRepository;
import com.importer.fileimporter.service.BinanceSpotActivityService;
import com.importer.fileimporter.service.MexcSpotActivityService;
import com.importer.fileimporter.service.EncryptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/exchange")
@RequiredArgsConstructor
@Tag(name = "Exchange Configuration", description = "Endpoints for managing exchange API keys")
public class ExchangeConfigController {

    private final UserExchangeConfigRepository userExchangeConfigRepository;
    private final EncryptionService encryptionService;
    private final BinanceSpotActivityService binanceSpotActivityService;
    private final MexcSpotActivityService mexcSpotActivityService;
    private final com.importer.fileimporter.service.PortfolioService portfolioService;
    private final com.importer.fileimporter.service.IolApiService iolApiService;

    @PostMapping("/config")
    @Operation(summary = "Save or update exchange API configuration")
    public ResponseEntity<?> saveConfig(@RequestBody ExchangeConfigRequest request) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        UserExchangeConfig config = userExchangeConfigRepository.findByUserAndExchangeName(user, request.getExchangeName())
                .orElse(UserExchangeConfig.builder()
                        .user(user)
                        .exchangeName(request.getExchangeName())
                        .build());

        config.setApiKey(request.getApiKey());
        config.setApiSecret(encryptionService.encrypt(request.getApiSecret()));
        
        userExchangeConfigRepository.save(config);

        // Auto-create portfolio named after the exchange if it doesn't exist
        String portfolioName = request.getExchangeName().name();
        portfolioService.getByNameForUser(portfolioName, user)
                .orElseGet(() -> portfolioService.findOrSave(portfolioName, request.getExchangeName()));
        
        return ResponseEntity.ok("Configuration saved successfully");
    }

    @GetMapping("/config")
    @Operation(summary = "Get current user exchange configurations")
    public ResponseEntity<List<ExchangeConfigResponse>> getConfigs() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        List<ExchangeConfigResponse> responses = userExchangeConfigRepository.findByUser(user).stream()
                .map(config -> ExchangeConfigResponse.builder()
                        .exchangeName(config.getExchangeName())
                        .apiKey(config.getApiKey())
                        .lastSyncTimestamp(config.getLastSyncTimestamp())
                        .build())
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/binance/spot-activity")
    @Operation(summary = "Get fresh Binance spot balances and trade activity")
    public ResponseEntity<BinanceSpotActivityResponse> getBinanceSpotActivity() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(binanceSpotActivityService.getSpotActivity(user));
    }

    @GetMapping("/mexc/spot-activity")
    @Operation(summary = "Get fresh MexC spot balances and trade activity")
    public ResponseEntity<MexcSpotActivityResponse> getMexcSpotActivity() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(mexcSpotActivityService.getSpotActivity(user));
    }

    @PostMapping("/iol/login")
    @Operation(summary = "Authenticate with IOL and get a bearer token")
    public ResponseEntity<com.importer.fileimporter.dto.integration.iol.IolTokenResponse> loginIol() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(iolApiService.loginForUser(user));
    }
}
