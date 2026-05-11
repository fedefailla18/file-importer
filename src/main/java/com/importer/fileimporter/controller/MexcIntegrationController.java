package com.importer.fileimporter.controller;

import com.importer.fileimporter.dto.integration.mexc.MexcOrderResponse;
import com.importer.fileimporter.entity.MexcRawOrder;
import com.importer.fileimporter.entity.MexcSyncProgress;
import com.importer.fileimporter.entity.ExchangeName;
import com.importer.fileimporter.entity.User;
import com.importer.fileimporter.entity.UserExchangeConfig;
import com.importer.fileimporter.repository.MexcRawOrderRepository;
import com.importer.fileimporter.repository.MexcSyncProgressRepository;
import com.importer.fileimporter.repository.UserExchangeConfigRepository;
import com.importer.fileimporter.service.MexcApiService;
import com.importer.fileimporter.service.MexcAsyncSyncService;
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
@RequestMapping("/api/integration/mexc")
@RequiredArgsConstructor
@Tag(name = "MexC Integration", description = "Direct endpoints to query MexC API and sync orders")
public class MexcIntegrationController {

    private final MexcApiService mexcApiService;
    private final UserExchangeConfigRepository userExchangeConfigRepository;
    private final EncryptionService encryptionService;
    private final MexcAsyncSyncService mexcAsyncSyncService;
    private final MexcRawOrderRepository rawOrderRepository;
    private final MexcSyncProgressRepository syncProgressRepository;

    @GetMapping("/orders")
    @Operation(summary = "Get all orders for a symbol directly from MexC (Proxied)")
    public ResponseEntity<List<MexcOrderResponse>> getAllOrdersProxy(
            @AuthenticationPrincipal User user,
            @Parameter(description = "Trading symbol (e.g., BTCUSDT)", required = true) @RequestParam String symbol,
            @Parameter(description = "Start time in epoch ms") @RequestParam(required = false) Long startTime,
            @Parameter(description = "End time in epoch ms") @RequestParam(required = false) Long endTime,
            @Parameter(description = "Order ID to fetch from") @RequestParam(required = false) Long orderId) {

        UserExchangeConfig config = getMexcConfig(user);
        String apiKey = config.getApiKey();
        String secretKey = encryptionService.decrypt(config.getApiSecret());

        List<MexcOrderResponse> orders = mexcApiService.getAllOrders(apiKey, secretKey, symbol, startTime, endTime, orderId);

        return ResponseEntity.ok(orders);
    }

    @PostMapping("/sync-all")
    @Operation(summary = "Trigger background exhaustive sync of all orders for all traded symbols (MexC)")
    public ResponseEntity<String> triggerExhaustiveSync(@AuthenticationPrincipal User user) {
        getMexcConfig(user);
        mexcAsyncSyncService.runFullOrderSync(user);
        return ResponseEntity.accepted().body("Exhaustive MexC order sync task started in background");
    }

    @GetMapping("/sync-status")
    @Operation(summary = "Check the MexC sync progress for all symbols")
    public ResponseEntity<List<MexcSyncProgress>> getSyncStatus(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(syncProgressRepository.findByUser(user)); 
    }

    @GetMapping("/raw-orders")
    @Operation(summary = "Fetch previously synced raw MexC orders from database")
    public ResponseEntity<Page<MexcRawOrder>> getRawOrders(
            @AuthenticationPrincipal User user,
            @Parameter(description = "Trading symbol (e.g., BTCUSDT)", required = true) @RequestParam String symbol,
            @PageableDefault(size = 50) Pageable pageable) {
        
        Page<MexcRawOrder> orders = rawOrderRepository.findByUserAndSymbol(user, symbol, pageable);
        return ResponseEntity.ok(orders);
    }

    private UserExchangeConfig getMexcConfig(User user) {
        return userExchangeConfigRepository.findByUserAndExchangeName(user, ExchangeName.MEXC)
                .orElseThrow(() -> new IllegalArgumentException("MexC API keys not configured for user"));
    }
}
