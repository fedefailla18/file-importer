package com.importer.fileimporter.controller;

import com.importer.fileimporter.dto.integration.binance.BinanceDepositResponse;
import com.importer.fileimporter.dto.integration.binance.BinanceFiatOrderResponse;
import com.importer.fileimporter.dto.integration.binance.BinanceOrderResponse;
import com.importer.fileimporter.dto.integration.binance.BinanceTradeResponse;
import com.importer.fileimporter.dto.integration.binance.BinanceWithdrawResponse;
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

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
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
            @Parameter(description = "Start date (yyyy-MM-dd), defaults to 2020-01-01") @RequestParam(required = false) String startDate,
            @Parameter(description = "End time in epoch ms") @RequestParam(required = false) Long endTime) {

        UserExchangeConfig config = getBinanceConfig(user);
        String apiKey = config.getApiKey();
        String secretKey = encryptionService.decrypt(config.getApiSecret());

        long startMillis = parseStartDate(startDate);
        long endMillis = endTime != null ? endTime : System.currentTimeMillis();

        // /api/v3/allOrders 400s if both startTime and endTime are set and more than 24h
        // apart (confirmed against Binance's own docs, 2026-09-12 — this endpoint never
        // actually tolerated the SIX_MONTHS_MS window it used to loop with). Page by
        // orderId instead — startTime only on the first call, then orderId cursor — and
        // filter the requested [startMillis, endMillis] window in-memory afterward.
        List<BinanceOrderResponse> allOrders = new ArrayList<>();
        Long lastOrderId = null;
        while (true) {
            List<BinanceOrderResponse> page = lastOrderId == null
                    ? binanceApiService.getAllOrders(apiKey, secretKey, symbol, startMillis, null, null)
                    : binanceApiService.getAllOrders(apiKey, secretKey, symbol, null, null, lastOrderId + 1);
            if (page == null || page.isEmpty()) break;
            allOrders.addAll(page);
            lastOrderId = page.get(page.size() - 1).getOrderId();
            if (page.size() < ORDERS_PAGE_LIMIT) break;
        }

        List<BinanceOrderResponse> inWindow = allOrders.stream()
                .filter(o -> o.getTime() != null && o.getTime() >= startMillis && o.getTime() <= endMillis)
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(inWindow);
    }

    @GetMapping("/my-trades")
    @Operation(summary = "Get all trades for a symbol directly from Binance (Proxied)")
    public ResponseEntity<List<BinanceTradeResponse>> getMyTradesProxy(
            @AuthenticationPrincipal User user,
            @Parameter(description = "Trading symbol (e.g., BTCUSDT)", required = true) @RequestParam String symbol,
            @Parameter(description = "Start date (yyyy-MM-dd), defaults to 2020-01-01") @RequestParam(required = false) String startDate,
            @Parameter(description = "End time in epoch ms") @RequestParam(required = false) Long endTime) {

        UserExchangeConfig config = getBinanceConfig(user);
        String apiKey = config.getApiKey();
        String secretKey = encryptionService.decrypt(config.getApiSecret());

        long startMillis = parseStartDate(startDate);
        long endMillis = endTime != null ? endTime : System.currentTimeMillis();

        // /api/v3/myTrades 400s if both startTime and endTime are set and more than 24h
        // apart — this is exactly the bug reported 2026-09-12 (real Binance error log,
        // confirmed against Binance's own docs: "time between startTime and endTime can't
        // be longer than 24 hours"). SIX_MONTHS_MS was never a valid window for this
        // endpoint. Page by trade id instead, like BinanceApiService.getAllMyTrades()/
        // BinanceFullSyncService.syncSpotTrades() already do correctly elsewhere in this
        // codebase — startTime only on the first call, then id cursor — and filter the
        // requested [startMillis, endMillis] window in-memory afterward.
        List<BinanceTradeResponse> allTrades = new ArrayList<>();
        Long lastTradeId = null;
        while (true) {
            List<BinanceTradeResponse> page = lastTradeId == null
                    ? binanceApiService.getMyTrades(apiKey, secretKey, symbol, startMillis, null, null)
                    : binanceApiService.getMyTrades(apiKey, secretKey, symbol, null, null, lastTradeId + 1);
            if (page == null || page.isEmpty()) break;
            allTrades.addAll(page);
            lastTradeId = page.get(page.size() - 1).getId();
            if (page.size() < ORDERS_PAGE_LIMIT) break;
        }

        List<BinanceTradeResponse> inWindow = allTrades.stream()
                .filter(t -> t.getTime() != null && t.getTime() >= startMillis && t.getTime() <= endMillis)
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(inWindow);
    }

    @GetMapping("/deposits")
    @Operation(summary = "Get crypto deposit history (Proxied)")
    public ResponseEntity<List<BinanceDepositResponse>> getDepositHistoryProxy(
            @AuthenticationPrincipal User user,
            @Parameter(description = "Start date (yyyy-MM-dd), defaults to 2020-01-01") @RequestParam(required = false) String startDate,
            @Parameter(description = "End time in epoch ms") @RequestParam(required = false) Long endTime) {

        UserExchangeConfig config = getBinanceConfig(user);
        String apiKey = config.getApiKey();
        String secretKey = encryptionService.decrypt(config.getApiSecret());

        long startMillis = parseStartDate(startDate);
        long endMillis = endTime != null ? endTime : System.currentTimeMillis();

        List<BinanceDepositResponse> allDeposits = new ArrayList<>();
        long currentStart = startMillis;

        // Binance caps deposit history requests at a 90-day window when both timestamps
        // are set (confirmed against Binance's own docs, 2026-09-12) — was SIX_MONTHS_MS
        // (180 days) here, which would 400 the same way /myTrades did above.
        while (currentStart < endMillis) {
            long currentEnd = Math.min(currentStart + NINETY_DAYS_MS, endMillis);
            List<BinanceDepositResponse> deposits = binanceApiService.getDepositHistory(apiKey, secretKey, currentStart, currentEnd);
            if (deposits != null) {
                allDeposits.addAll(deposits);
            }
            currentStart = currentEnd;
        }

        return ResponseEntity.ok(allDeposits);
    }

    @GetMapping("/withdrawals")
    @Operation(summary = "Get crypto withdrawal history (Proxied)")
    public ResponseEntity<List<BinanceWithdrawResponse>> getWithdrawHistoryProxy(
            @AuthenticationPrincipal User user,
            @Parameter(description = "Start date (yyyy-MM-dd), defaults to 2020-01-01") @RequestParam(required = false) String startDate,
            @Parameter(description = "End time in epoch ms") @RequestParam(required = false) Long endTime) {

        UserExchangeConfig config = getBinanceConfig(user);
        String apiKey = config.getApiKey();
        String secretKey = encryptionService.decrypt(config.getApiSecret());

        long startMillis = parseStartDate(startDate);
        long endMillis = endTime != null ? endTime : System.currentTimeMillis();

        List<BinanceWithdrawResponse> allWithdrawals = new ArrayList<>();
        long currentStart = startMillis;

        // Same 90-day Binance limit as deposit history, above.
        while (currentStart < endMillis) {
            long currentEnd = Math.min(currentStart + NINETY_DAYS_MS, endMillis);
            List<BinanceWithdrawResponse> withdrawals = binanceApiService.getWithdrawHistory(apiKey, secretKey, currentStart, currentEnd);
            if (withdrawals != null) {
                allWithdrawals.addAll(withdrawals);
            }
            currentStart = currentEnd;
        }

        return ResponseEntity.ok(allWithdrawals);
    }

    @GetMapping("/fiat-orders")
    @Operation(summary = "Get fiat deposit/withdraw history (Proxied)")
    public ResponseEntity<BinanceFiatOrderResponse> getFiatOrdersProxy(
            @AuthenticationPrincipal User user,
            @Parameter(description = "Transaction type (0: deposit, 1: withdraw)", required = true) @RequestParam int transactionType,
            @Parameter(description = "Start date (yyyy-MM-dd), defaults to 2020-01-01") @RequestParam(required = false) String startDate,
            @Parameter(description = "End time in epoch ms") @RequestParam(required = false) Long endTime) {

        UserExchangeConfig config = getBinanceConfig(user);
        String apiKey = config.getApiKey();
        String secretKey = encryptionService.decrypt(config.getApiSecret());

        long startMillis = parseStartDate(startDate);
        long endMillis = endTime != null ? endTime : System.currentTimeMillis();

        BinanceFiatOrderResponse finalResponse = new BinanceFiatOrderResponse();
        finalResponse.setData(new ArrayList<>());
        finalResponse.setSuccess(true);
        long currentStart = startMillis;

        while (currentStart < endMillis) {
            long currentEnd = Math.min(currentStart + SIX_MONTHS_MS, endMillis);
            BinanceFiatOrderResponse resp = binanceApiService.getFiatOrders(apiKey, secretKey, transactionType, currentStart, currentEnd);
            if (resp != null && resp.getData() != null) {
                finalResponse.getData().addAll(resp.getData());
            }
            currentStart = currentEnd;
        }
        finalResponse.setTotal(finalResponse.getData().size());

        return ResponseEntity.ok(finalResponse);
    }

    @GetMapping("/fiat-payments")
    @Operation(summary = "Get fiat payment history (Proxied)")
    public ResponseEntity<BinanceFiatOrderResponse> getFiatPaymentsProxy(
            @AuthenticationPrincipal User user,
            @Parameter(description = "Transaction type (0: buy, 1: sell)", required = true) @RequestParam int transactionType,
            @Parameter(description = "Start date (yyyy-MM-dd), defaults to 2020-01-01") @RequestParam(required = false) String startDate,
            @Parameter(description = "End time in epoch ms") @RequestParam(required = false) Long endTime) {

        UserExchangeConfig config = getBinanceConfig(user);
        String apiKey = config.getApiKey();
        String secretKey = encryptionService.decrypt(config.getApiSecret());

        long startMillis = parseStartDate(startDate);
        long endMillis = endTime != null ? endTime : System.currentTimeMillis();

        BinanceFiatOrderResponse finalResponse = new BinanceFiatOrderResponse();
        finalResponse.setData(new ArrayList<>());
        finalResponse.setSuccess(true);
        long currentStart = startMillis;

        while (currentStart < endMillis) {
            long currentEnd = Math.min(currentStart + SIX_MONTHS_MS, endMillis);
            BinanceFiatOrderResponse resp = binanceApiService.getFiatPayments(apiKey, secretKey, transactionType, currentStart, currentEnd);
            if (resp != null && resp.getData() != null) {
                finalResponse.getData().addAll(resp.getData());
            }
            currentStart = currentEnd;
        }
        finalResponse.setTotal(finalResponse.getData().size());

        return ResponseEntity.ok(finalResponse);
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

    // Used for /fiat-orders and /fiat-payments only now — Binance doesn't document a hard
    // error for a wide window on these two (unlike myTrades/allOrders/deposit/withdraw,
    // all fixed above), just an undocumented ~90-day practical data retention some users
    // have reported. Left as-is since there's no confirmed 400 here; revisit if one shows up.
    private static final long SIX_MONTHS_MS = 180L * 24 * 60 * 60 * 1000;
    private static final long NINETY_DAYS_MS = 90L * 24 * 60 * 60 * 1000;
    private static final int ORDERS_PAGE_LIMIT = 1000;
    private static final String DEFAULT_START_DATE = "2020-01-01";

    private long parseStartDate(String startDateStr) {
        if (startDateStr == null || startDateStr.isEmpty()) {
            startDateStr = DEFAULT_START_DATE;
        }
        return LocalDate.parse(startDateStr).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }
}
