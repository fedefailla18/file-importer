package com.importer.fileimporter.service;

import com.importer.fileimporter.dto.integration.mexc.MexcAccountResponse;
import com.importer.fileimporter.dto.integration.mexc.MexcTradeResponse;
import com.importer.fileimporter.entity.ExchangeName;
import com.importer.fileimporter.entity.User;
import com.importer.fileimporter.entity.UserExchangeConfig;
import com.importer.fileimporter.payload.response.MexcAssetBalanceResponse;
import com.importer.fileimporter.payload.response.MexcSpotActivityResponse;
import com.importer.fileimporter.payload.response.MexcSpotActivitySummaryResponse;
import com.importer.fileimporter.payload.response.MexcSpotTradeRowResponse;
import com.importer.fileimporter.repository.UserExchangeConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
public class MexcSpotActivityService {

    private static final Set<String> QUOTE_CURRENCIES = Set.of(
            "USDT", "USDC", "BTC", "ETH", "BNB"
    );
    private static final List<String> QUOTE_CURRENCIES_ORDERED = List.of(
            "USDT", "BTC", "ETH", "BNB"
    );

    private final MexcApiService mexcApiService;
    private final UserExchangeConfigRepository userExchangeConfigRepository;
    private final EncryptionService encryptionService;

    public MexcSpotActivityResponse getSpotActivity(User user) {
        UserExchangeConfig config = userExchangeConfigRepository.findByUserAndExchangeName(user, ExchangeName.MEXC)
                .orElseThrow(() -> new IllegalArgumentException("MexC API keys not configured for user"));

        String apiKey = config.getApiKey();
        String secretKey = encryptionService.decrypt(config.getApiSecret());

        MexcAccountResponse accountInfo = mexcApiService.getAccountInfo(apiKey, secretKey);
        List<MexcAssetBalanceResponse> balances = mapNonZeroBalances(accountInfo);
        Set<String> investmentAssets = balances.stream()
                .map(MexcAssetBalanceResponse::getAsset)
                .filter(asset -> !QUOTE_CURRENCIES.contains(asset))
                .collect(Collectors.toSet());

        List<String[]> candidatePairs = buildCandidatePairs(investmentAssets);
        Map<String, MexcSpotTradeRowResponse> uniqueTrades = new LinkedHashMap<>();

        for (String[] pair : candidatePairs) {
            String symbol = pair[0];
            String baseAsset = pair[1];
            String quoteAsset = pair[2];

            try {
                List<MexcTradeResponse> trades = mexcApiService.getMyTrades(apiKey, secretKey, symbol, null, null, null);
                for (MexcTradeResponse trade : trades) {
                    MexcSpotTradeRowResponse row = MexcSpotTradeRowResponse.builder()
                            .symbol(trade.getSymbol())
                            .baseAsset(baseAsset)
                            .quoteAsset(quoteAsset)
                            .side(Boolean.TRUE.equals(trade.getIsBuyer()) ? "BUY" : "SELL")
                            .tradeId(trade.getId())
                            .orderId(trade.getOrderId())
                            .price(trade.getPrice())
                            .qty(trade.getQty())
                            .quoteQty(trade.getQuoteQty())
                            .commission(trade.getCommission())
                            .commissionAsset(trade.getCommissionAsset())
                            .time(trade.getTime())
                            .buyer(trade.getIsBuyer())
                            .maker(trade.getIsMaker())
                            .build();

                    uniqueTrades.put(buildTradeKey(row), row);
                }
            } catch (Exception e) {
                log.error("Error loading MexC spot activity for {}: {}", symbol, e.getMessage());
            }
        }

        List<MexcSpotTradeRowResponse> trades = uniqueTrades.values().stream()
                .sorted(Comparator.comparing(MexcSpotTradeRowResponse::getTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(MexcSpotTradeRowResponse::getTradeId, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        BigDecimal grossBuyQuoteQty = trades.stream()
                .filter(trade -> "BUY".equals(trade.getSide()))
                .map(MexcSpotTradeRowResponse::getQuoteQty)
                .filter(qty -> qty != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal grossSellQuoteQty = trades.stream()
                .filter(trade -> "SELL".equals(trade.getSide()))
                .map(MexcSpotTradeRowResponse::getQuoteQty)
                .filter(qty -> qty != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        MexcSpotActivitySummaryResponse summary = MexcSpotActivitySummaryResponse.builder()
                .activeAssetCount(balances.size())
                .candidatePairCount(candidatePairs.size())
                .symbolCountWithTrades((int) trades.stream().map(MexcSpotTradeRowResponse::getSymbol).distinct().count())
                .totalTradeCount(trades.size())
                .buyTradeCount((int) trades.stream().filter(trade -> "BUY".equals(trade.getSide())).count())
                .sellTradeCount((int) trades.stream().filter(trade -> "SELL".equals(trade.getSide())).count())
                .grossBuyQuoteQty(grossBuyQuoteQty)
                .grossSellQuoteQty(grossSellQuoteQty)
                .fetchedAt(System.currentTimeMillis())
                .lastSyncTimestamp(config.getLastSyncTimestamp())
                .build();

        return MexcSpotActivityResponse.builder()
                .summary(summary)
                .balances(balances)
                .trades(trades)
                .build();
    }

    private List<MexcAssetBalanceResponse> mapNonZeroBalances(MexcAccountResponse accountInfo) {
        if (accountInfo == null || accountInfo.getBalances() == null) {
            return List.of();
        }

        return accountInfo.getBalances().stream()
                .filter(balance -> balance.getFree().add(balance.getLocked()).compareTo(BigDecimal.ZERO) > 0)
                .map(balance -> MexcAssetBalanceResponse.builder()
                        .asset(balance.getAsset())
                        .free(balance.getFree())
                        .locked(balance.getLocked())
                        .total(balance.getFree().add(balance.getLocked()))
                        .build())
                .sorted(Comparator.comparing(MexcAssetBalanceResponse::getTotal, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    private List<String[]> buildCandidatePairs(Set<String> investmentAssets) {
        List<String[]> candidates = new ArrayList<>();
        for (String asset : investmentAssets) {
            for (String quote : QUOTE_CURRENCIES_ORDERED) {
                if (!asset.equals(quote)) {
                    candidates.add(new String[]{asset + quote, asset, quote});
                }
            }
        }
        return candidates;
    }

    private String buildTradeKey(MexcSpotTradeRowResponse trade) {
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
