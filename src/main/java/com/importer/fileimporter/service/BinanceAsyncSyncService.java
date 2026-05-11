package com.importer.fileimporter.service;

import com.importer.fileimporter.dto.integration.binance.BinanceAccountResponse;
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
public class BinanceAsyncSyncService {

    private final BinanceApiService binanceApiService;
    private final BinanceOrderSyncService binanceOrderSyncService;
    private final BinanceFullSyncService binanceFullSyncService;
    private final SyncNotificationService syncNotificationService;
    private final UserExchangeConfigRepository userExchangeConfigRepository;
    private final EncryptionService encryptionService;

    @Async("syncTaskExecutor")
    public void runFullOrderSync(User user) {
        log.info("Starting background exhaustive order sync for user: {}", user.getUsername());
        
        UserExchangeConfig config = userExchangeConfigRepository.findByUserAndExchangeName(user, ExchangeName.BINANCE)
                .orElseThrow(() -> new IllegalArgumentException("Binance API keys not configured"));

        String apiKey = config.getApiKey();
        String secretKey = encryptionService.decrypt(config.getApiSecret());

        try {
            // 1. Symbol Discovery
            BinanceAccountResponse account = binanceApiService.getAccountInfo(apiKey, secretKey);
            Set<String> assets = account.getBalances().stream()
                    .filter(b -> b.getFree().add(b.getLocked()).compareTo(BigDecimal.ZERO) > 0)
                    .map(BinanceAccountResponse.AssetBalance::getAsset)
                    .collect(Collectors.toSet());

            log.info("Found {} assets with balance. Discovering pairs...", assets.size());

            BinanceExchangeInfoResponse exchangeInfo = binanceApiService.getExchangeInfo();
            List<String> relevantSymbols = exchangeInfo.getSymbols().stream()
                    .filter(s -> assets.contains(s.getBaseAsset()) || assets.contains(s.getQuoteAsset()))
                    .map(BinanceExchangeInfoResponse.SymbolInfo::getSymbol)
                    .collect(Collectors.toList());

            log.info("Starting exhaustive sync for {} symbols", relevantSymbols.size());

            // 2. Exhaustive sync per symbol
            for (String symbol : relevantSymbols) {
                try {
                    binanceOrderSyncService.syncOrdersForSymbol(user, symbol, apiKey, secretKey);
                } catch (Exception e) {
                    log.error("Failed to sync symbol {}: {}", symbol, e.getMessage());
                }
            }

            log.info("Background exhaustive order sync completed for user: {}", user.getUsername());
        } catch (Exception e) {
            log.error("Critical failure in background sync for user {}: {}", user.getUsername(), e.getMessage());
        }
    }

    @Async("syncTaskExecutor")
    public void syncFullHistoryAsync(User user, String portfolioName, Long startDate, Long endDate) {
        log.info("Starting background full history sync for user: {} portfolio: {}", user.getUsername(), portfolioName);
        try {
            binanceFullSyncService.syncFullHistory(user, portfolioName, startDate, endDate);
            log.info("Background full history sync completed for user: {}", user.getUsername());
            syncNotificationService.notifyCompleted(user.getUsername(), portfolioName);
        } catch (Exception e) {
            log.error("Failed background full history sync for user {}: {}", user.getUsername(), e.getMessage(), e);
            syncNotificationService.notifyFailed(user.getUsername(), portfolioName, e.getMessage());
        }
    }
}
