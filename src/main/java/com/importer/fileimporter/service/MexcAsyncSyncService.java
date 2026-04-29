package com.importer.fileimporter.service;

import com.importer.fileimporter.dto.integration.mexc.MexcAccountResponse;
import com.importer.fileimporter.dto.integration.binance.BinanceExchangeInfoResponse;
import com.importer.fileimporter.entity.ExchangeName;
import com.importer.fileimporter.entity.User;
import com.importer.fileimporter.entity.UserExchangeConfig;
import com.importer.fileimporter.repository.UserExchangeConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MexcAsyncSyncService {

    private final MexcApiService mexcApiService;
    private final MexcOrderSyncService mexcOrderSyncService;
    private final MexcFullSyncService mexcFullSyncService;
    private final SyncNotificationService syncNotificationService;
    private final UserExchangeConfigRepository userExchangeConfigRepository;
    private final EncryptionService encryptionService;

    @Async("syncTaskExecutor")
    public void runFullOrderSync(User user) {
        log.info("Starting background exhaustive MexC order sync for user: {}", user.getUsername());
        
        UserExchangeConfig config = userExchangeConfigRepository.findByUserAndExchangeName(user, ExchangeName.MEXC)
                .orElseThrow(() -> new IllegalArgumentException("MexC API keys not configured"));

        String apiKey = config.getApiKey();
        String secretKey = encryptionService.decrypt(config.getApiSecret());

        try {
            MexcAccountResponse account = mexcApiService.getAccountInfo(apiKey, secretKey);
            Set<String> assets = account.getBalances().stream()
                    .filter(b -> b.getFree().add(b.getLocked()).compareTo(BigDecimal.ZERO) > 0)
                    .map(MexcAccountResponse.AssetBalance::getAsset)
                    .collect(Collectors.toSet());

            BinanceExchangeInfoResponse exchangeInfo = mexcApiService.getExchangeInfo();
            List<String> relevantSymbols = exchangeInfo.getSymbols().stream()
                    .filter(s -> assets.contains(s.getBaseAsset()) || assets.contains(s.getQuoteAsset()))
                    .map(BinanceExchangeInfoResponse.SymbolInfo::getSymbol)
                    .collect(Collectors.toList());

            for (String symbol : relevantSymbols) {
                try {
                    mexcOrderSyncService.syncOrdersForSymbol(user, symbol, apiKey, secretKey);
                } catch (Exception e) {
                    log.error("Failed to sync MexC symbol {}: {}", symbol, e.getMessage());
                }
            }

            log.info("Background exhaustive MexC order sync completed for user: {}", user.getUsername());
        } catch (Exception e) {
            log.error("Critical failure in background MexC sync for user {}: {}", user.getUsername(), e.getMessage());
        }
    }

    @Async("syncTaskExecutor")
    public void syncFullHistoryAsync(User user, String portfolioName, Long startDate, Long endDate) {
        log.info("Starting background MexC full history sync for user: {} portfolio: {}", user.getUsername(), portfolioName);
        try {
            mexcFullSyncService.syncFullHistory(user, portfolioName, startDate, endDate);
            log.info("Background MexC full history sync completed for user: {}", user.getUsername());
            syncNotificationService.notifyCompleted(user.getUsername(), portfolioName);
        } catch (Exception e) {
            log.error("Failed background MexC full history sync for user {}: {}", user.getUsername(), e.getMessage(), e);
            syncNotificationService.notifyFailed(user.getUsername(), portfolioName, e.getMessage());
        }
    }
}
