package com.importer.fileimporter.controller;

import com.importer.fileimporter.dto.integration.binance.BinanceOrderResponse;
import com.importer.fileimporter.entity.BinanceRawOrder;
import com.importer.fileimporter.entity.BinanceSyncProgress;
import com.importer.fileimporter.entity.ExchangeName;
import com.importer.fileimporter.entity.User;
import com.importer.fileimporter.entity.UserExchangeConfig;
import com.importer.fileimporter.repository.BinanceRawOrderRepository;
import com.importer.fileimporter.repository.BinanceSyncProgressRepository;
import com.importer.fileimporter.repository.UserExchangeConfigRepository;
import com.importer.fileimporter.service.BinanceApiService;
import com.importer.fileimporter.service.BinanceAsyncSyncService;
import com.importer.fileimporter.service.EncryptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/integration/binance")
@RequiredArgsConstructor
@Tag(name = "Binance Integration", description = "Direct endpoints to query Binance API and sync orders")
public class BinanceIntegrationController {

    private final BinanceApiService binanceApiService;
    private final UserExchangeConfigRepository userExchangeConfigRepository;
    private final EncryptionService encryptionService;
    private final BinanceAsyncSyncService binanceAsyncSyncService;
    private final BinanceRawOrderRepository rawOrderRepository;
    private final BinanceSyncProgressRepository syncProgressRepository;

    @GetMapping("/orders")
    @Operation(summary = "Get all orders for a symbol directly from Binance (Proxied)")
    public ResponseEntity<List<BinanceOrderResponse>> getAllOrdersProxy(
            @AuthenticationPrincipal User user,
            @Parameter(description = "Trading symbol (e.g., BTCUSDT)", required = true) @RequestParam String symbol,
            @Parameter(description = "Start time in epoch ms") @RequestParam(required = false) Long startTime,
            @Parameter(description = "End time in epoch ms") @RequestParam(required = false) Long endTime,
            @Parameter(description = "Order ID to fetch from") @RequestParam(required = false) Long orderId) {

        UserExchangeConfig config = getBinanceConfig(user);
        String apiKey = config.getApiKey();
        String secretKey = encryptionService.decrypt(config.getApiSecret());

        List<BinanceOrderResponse> orders = binanceApiService.getAllOrders(apiKey, secretKey, symbol, startTime, endTime, orderId);

        return ResponseEntity.ok(orders);
    }

    @PostMapping("/sync-all")
    @Operation(summary = "Trigger background exhaustive sync of all orders for all traded symbols")
    public ResponseEntity<String> triggerExhaustiveSync(@AuthenticationPrincipal User user) {
        getBinanceConfig(user);
        binanceAsyncSyncService.runFullOrderSync(user);
        return ResponseEntity.accepted().body("Exhaustive order sync task started in background");
    }

    @GetMapping("/sync-status")
    @Operation(summary = "Check the sync progress for all symbols")
    public ResponseEntity<List<BinanceSyncProgress>> getSyncStatus(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(syncProgressRepository.findByUser(user)); 
    }

    @GetMapping("/raw-orders")
    @Operation(summary = "Fetch previously synced raw orders from database")
    public ResponseEntity<Page<BinanceRawOrder>> getRawOrders(
            @AuthenticationPrincipal User user,
            @Parameter(description = "Trading symbol (e.g., BTCUSDT)", required = true) @RequestParam String symbol,
            @PageableDefault(size = 50) Pageable pageable) {
        
        Page<BinanceRawOrder> orders = rawOrderRepository.findByUserAndSymbol(user, symbol, pageable);
        return ResponseEntity.ok(orders);
    }

    private UserExchangeConfig getBinanceConfig(User user) {
        return userExchangeConfigRepository.findByUserAndExchangeName(user, ExchangeName.BINANCE)
                .orElseThrow(() -> new IllegalArgumentException("Binance API keys not configured for user"));
    }
}
