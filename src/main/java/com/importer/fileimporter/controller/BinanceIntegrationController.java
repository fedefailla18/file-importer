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

        List<BinanceOrderResponse> allOrders = new ArrayList<>();
        long currentStart = startMillis;

        while (currentStart < endMillis) {
            long currentEnd = Math.min(currentStart + SIX_MONTHS_MS, endMillis);
            List<BinanceOrderResponse> orders = binanceApiService.getAllOrders(apiKey, secretKey, symbol, currentStart, currentEnd, null);
            if (orders != null) {
                allOrders.addAll(orders);
            }
            currentStart = currentEnd;
        }

        return ResponseEntity.ok(allOrders);
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

        List<BinanceTradeResponse> allTrades = new ArrayList<>();
        long currentStart = startMillis;

        while (currentStart < endMillis) {
            long currentEnd = Math.min(currentStart + SIX_MONTHS_MS, endMillis);
            List<BinanceTradeResponse> trades = binanceApiService.getMyTrades(apiKey, secretKey, symbol, currentStart, currentEnd, null);
            if (trades != null) {
                allTrades.addAll(trades);
            }
            currentStart = currentEnd;
        }

        return ResponseEntity.ok(allTrades);
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

        while (currentStart < endMillis) {
            long currentEnd = Math.min(currentStart + SIX_MONTHS_MS, endMillis);
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

        while (currentStart < endMillis) {
            long currentEnd = Math.min(currentStart + SIX_MONTHS_MS, endMillis);
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

    private static final long SIX_MONTHS_MS = 180L * 24 * 60 * 60 * 1000;
    private static final String DEFAULT_START_DATE = "2020-01-01";

    private long parseStartDate(String startDateStr) {
        if (startDateStr == null || startDateStr.isEmpty()) {
            startDateStr = DEFAULT_START_DATE;
        }
        return LocalDate.parse(startDateStr).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }
}
