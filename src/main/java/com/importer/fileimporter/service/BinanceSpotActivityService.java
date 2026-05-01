package com.importer.fileimporter.service;

import com.importer.fileimporter.dto.integration.binance.BinanceAccountResponse;
import com.importer.fileimporter.dto.integration.binance.BinanceExchangeInfoResponse;
import com.importer.fileimporter.dto.integration.binance.BinanceTradeResponse;
import com.importer.fileimporter.entity.ExchangeName;
import com.importer.fileimporter.entity.User;
import com.importer.fileimporter.entity.UserExchangeConfig;
import com.importer.fileimporter.payload.response.BinanceAssetBalanceResponse;
import com.importer.fileimporter.payload.response.BinanceSpotActivityResponse;
import com.importer.fileimporter.payload.response.BinanceSpotActivitySummaryResponse;
import com.importer.fileimporter.payload.response.BinanceSpotTradeRowResponse;
import com.importer.fileimporter.repository.UserExchangeConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BinanceSpotActivityService {

    private static final Set<String> QUOTE_CURRENCIES = Set.of(
            "USDT", "BUSD", "USDC", "BTC", "ETH", "BNB", "FDUSD", "USDS", "DAI", "TUSD", "EUR", "TRY"
    );
    private static final List<String> QUOTE_CURRENCIES_ORDERED = List.of(
            "USDT", "BTC", "ETH", "BNB", "BUSD", "USDC", "FDUSD"
    );
    private static final int BINANCE_MAX_PAGE_SIZE = 1000;
    private static final int CONSECUTIVE_FAILURE_THRESHOLD = 3;

    private final BinanceApiService binanceApiService;
    private final UserExchangeConfigRepository userExchangeConfigRepository;
    private final EncryptionService encryptionService;

    public BinanceSpotActivityResponse getSpotActivity(User user) {
        UserExchangeConfig config = userExchangeConfigRepository.findByUserAndExchangeName(user, ExchangeName.BINANCE)
                .orElseThrow(() -> new IllegalArgumentException("Binance API keys not configured for user"));

        String apiKey = config.getApiKey();
        String secretKey = encryptionService.decrypt(config.getApiSecret());

        BinanceAccountResponse accountInfo = binanceApiService.getAccountInfo(apiKey, secretKey);
        List<BinanceAssetBalanceResponse> balances = mapNonZeroBalances(accountInfo);
        Set<String> investmentAssets = balances.stream()
                .map(BinanceAssetBalanceResponse::getAsset)
                .filter(asset -> !QUOTE_CURRENCIES.contains(asset))
                .collect(Collectors.toSet());

        // Pre-fetch exchange info to avoid guessing pairs and getting 400s
        Set<String> validSymbols = binanceApiService.getExchangeInfo().getSymbols().stream()
                .map(BinanceExchangeInfoResponse.SymbolInfo::getSymbol)
                .collect(Collectors.toSet());

        List<String[]> candidatePairs = buildCandidatePairs(investmentAssets, validSymbols);
        Map<String, BinanceSpotTradeRowResponse> uniqueTrades = new LinkedHashMap<>();

        int consecutiveFailures = 0;

        for (String[] pair : candidatePairs) {
            String symbol = pair[0];
            String baseAsset = pair[1];
            String quoteAsset = pair[2];

            try {
                List<BinanceTradeResponse> trades = binanceApiService.getAllMyTrades(apiKey, secretKey, symbol);
                for (BinanceTradeResponse trade : trades) {
                    BinanceSpotTradeRowResponse row = BinanceSpotTradeRowResponse.builder()
                            .symbol(trade.getSymbol())
                            .baseAsset(baseAsset)
                            .quoteAsset(quoteAsset)
                            .side(Boolean.TRUE.equals(trade.getIsBuyer()) ? "BUY" : "SELL")
                            .tradeId(trade.getId())
                            .orderId(trade.getOrderId())
                            .orderListId(trade.getOrderListId())
                            .price(trade.getPrice())
                            .qty(trade.getQty())
                            .quoteQty(trade.getQuoteQty())
                            .commission(trade.getCommission())
                            .commissionAsset(trade.getCommissionAsset())
                            .time(trade.getTime())
                            .buyer(trade.getIsBuyer())
                            .maker(trade.getIsMaker())
                            .bestMatch(trade.getIsBestMatch())
                            .build();

                    uniqueTrades.put(buildTradeKey(row), row);
                }
                consecutiveFailures = 0; // Reset on success
            } catch (Exception e) {
                consecutiveFailures++;
                log.error("Error loading Binance spot activity for {} (Failure {}/{}): {}", 
                        symbol, consecutiveFailures, CONSECUTIVE_FAILURE_THRESHOLD, e.getMessage());
                
                if (consecutiveFailures >= CONSECUTIVE_FAILURE_THRESHOLD) {
                    log.error("Binance Sync aborted: Too many consecutive failures.");
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, 
                        "Binance synchronization failed multiple times. Please verify your API keys and permissions, or contact support if the issue persists.");
                }
            }
        }

        List<BinanceSpotTradeRowResponse> trades = uniqueTrades.values().stream()
                .sorted(Comparator.comparing(BinanceSpotTradeRowResponse::getTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(BinanceSpotTradeRowResponse::getTradeId, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        BigDecimal grossBuyQuoteQty = trades.stream()
                .filter(trade -> "BUY".equals(trade.getSide()))
                .map(BinanceSpotTradeRowResponse::getQuoteQty)
                .filter(qty -> qty != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal grossSellQuoteQty = trades.stream()
                .filter(trade -> "SELL".equals(trade.getSide()))
                .map(BinanceSpotTradeRowResponse::getQuoteQty)
                .filter(qty -> qty != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BinanceSpotActivitySummaryResponse summary = BinanceSpotActivitySummaryResponse.builder()
                .activeAssetCount(balances.size())
                .candidatePairCount(candidatePairs.size())
                .symbolCountWithTrades((int) trades.stream().map(BinanceSpotTradeRowResponse::getSymbol).distinct().count())
                .totalTradeCount(trades.size())
                .buyTradeCount((int) trades.stream().filter(trade -> "BUY".equals(trade.getSide())).count())
                .sellTradeCount((int) trades.stream().filter(trade -> "SELL".equals(trade.getSide())).count())
                .grossBuyQuoteQty(grossBuyQuoteQty)
                .grossSellQuoteQty(grossSellQuoteQty)
                .fetchedAt(System.currentTimeMillis())
                .lastSyncTimestamp(config.getLastSyncTimestamp())
                .build();

        return BinanceSpotActivityResponse.builder()
                .summary(summary)
                .balances(balances)
                .trades(trades)
                .build();
    }

    private List<BinanceAssetBalanceResponse> mapNonZeroBalances(BinanceAccountResponse accountInfo) {
        if (accountInfo == null || accountInfo.getBalances() == null) {
            return List.of();
        }

        return accountInfo.getBalances().stream()
                .filter(balance -> balance.getFree().add(balance.getLocked()).compareTo(BigDecimal.ZERO) > 0)
                .map(balance -> BinanceAssetBalanceResponse.builder()
                        .asset(balance.getAsset())
                        .free(balance.getFree())
                        .locked(balance.getLocked())
                        .total(balance.getFree().add(balance.getLocked()))
                        .build())
                .sorted(Comparator.comparing(BinanceAssetBalanceResponse::getTotal, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    private List<String[]> buildCandidatePairs(Set<String> investmentAssets, Set<String> validSymbols) {
        List<String[]> candidates = new ArrayList<>();
        for (String asset : investmentAssets) {
            for (String quote : QUOTE_CURRENCIES_ORDERED) {
                if (!asset.equals(quote)) {
                    String symbol = asset + quote;
                    if (validSymbols.contains(symbol)) {
                        candidates.add(new String[]{symbol, asset, quote});
                    }
                }
            }
        }
        return candidates;
    }

    private String buildTradeKey(BinanceSpotTradeRowResponse trade) {
        return String.join(":",
                safeString(trade.getSymbol()),
                safeString(trade.getTradeId()),
                safeString(trade.getOrderId()),
                safeString(trade.getTime()));
    }

    private String safeString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
