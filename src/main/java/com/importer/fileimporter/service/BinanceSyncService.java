package com.importer.fileimporter.service;

import com.importer.fileimporter.dto.BinanceApiTransactionAdapter;
import com.importer.fileimporter.dto.integration.binance.BinanceAccountResponse;
import com.importer.fileimporter.dto.integration.binance.BinanceTradeResponse;
import com.importer.fileimporter.entity.ExchangeName;
import com.importer.fileimporter.entity.Portfolio;
import com.importer.fileimporter.entity.Transaction;
import com.importer.fileimporter.entity.User;
import com.importer.fileimporter.entity.UserExchangeConfig;
import com.importer.fileimporter.repository.UserExchangeConfigRepository;
import com.importer.fileimporter.utils.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BinanceSyncService {

    private static final Set<String> QUOTE_CURRENCIES = Set.of(
            "USDT", "BUSD", "USDC", "BTC", "ETH", "BNB", "FDUSD", "USDS", "DAI", "TUSD", "EUR", "TRY"
    );
    private static final List<String> QUOTE_CURRENCIES_ORDERED = List.of(
            "USDT", "BTC", "ETH", "BNB", "BUSD", "USDC", "FDUSD"
    );
    // Package-private so unit tests can set it to 0 without Spring context
    long rateLimitDelayMs = 200L;

    private final BinanceApiService binanceApiService;
    private final UserExchangeConfigRepository userExchangeConfigRepository;
    private final EncryptionService encryptionService;
    private final TransactionProcessor transactionProcessor;
    private final PortfolioService portfolioService;

    @Transactional
    public void sync(User user, String portfolioName) {
        UserExchangeConfig config = userExchangeConfigRepository.findByUserAndExchangeName(user, ExchangeName.BINANCE)
                .orElseThrow(() -> new IllegalArgumentException("Binance API keys not configured for user"));

        String apiKey = config.getApiKey();
        String secretKey = encryptionService.decrypt(config.getApiSecret());

        Portfolio portfolio = portfolioService.findOrSave(portfolioName);

        long binanceServerTime = binanceApiService.getServerTime();
        long clockOffset = binanceServerTime - System.currentTimeMillis();
        log.info("Binance server time offset: {}ms", clockOffset);

        BinanceAccountResponse accountInfo = binanceApiService.getAccountInfo(apiKey, secretKey);
        Set<String> investmentAssets = accountInfo.getBalances().stream()
                .filter(b -> b.getFree().add(b.getLocked()).compareTo(BigDecimal.ZERO) > 0)
                .map(BinanceAccountResponse.AssetBalance::getAsset)
                .filter(asset -> !QUOTE_CURRENCIES.contains(asset))
                .collect(Collectors.toSet());

        log.info("Found {} investment assets for user {}: {}", investmentAssets.size(), user.getUsername(), investmentAssets);

        List<String[]> candidatePairs = buildCandidatePairs(investmentAssets);
        log.info("Checking {} candidate pairs", candidatePairs.size());

        Long lastSync = config.getLastSyncTimestamp();
        Long newSyncTimestamp = System.currentTimeMillis();

        for (String[] pair : candidatePairs) {
            String symbol = pair[0];
            String baseAsset = pair[1];
            String quoteAsset = pair[2];
            try {
                List<BinanceTradeResponse> trades = binanceApiService.getMyTrades(apiKey, secretKey, symbol, lastSync);
                if (trades != null && !trades.isEmpty()) {
                    log.info("Syncing {} trades for {}", trades.size(), symbol);
                    for (BinanceTradeResponse trade : trades) {
                        BinanceApiTransactionAdapter adapter = new BinanceApiTransactionAdapter(trade, baseAsset, quoteAsset);
                        Transaction transaction = mapToTransaction(adapter, portfolio);
                        transaction.setFeeSymbol(adapter.getFeeSymbol());
                        transactionProcessor.process(transaction);
                    }
                }
                Thread.sleep(rateLimitDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Binance sync interrupted");
                break;
            } catch (Exception e) {
                // -1121 = invalid symbol; expected for non-existent pairs, not an error
                if (e.getMessage() != null && e.getMessage().contains("-1121")) {
                    log.debug("Symbol {} does not exist on Binance, skipping", symbol);
                } else {
                    log.error("Error syncing trades for {}: {}", symbol, e.getMessage());
                }
            }
        }

        config.setLastSyncTimestamp(newSyncTimestamp);
        userExchangeConfigRepository.save(config);
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

    private Transaction mapToTransaction(BinanceApiTransactionAdapter data, Portfolio portfolio) {
        return Transaction.builder()
                .dateUtc(DateUtils.getLocalDateTime(data.getDate()))
                .pair(data.getPair())
                .executed(data.getExecuted())
                .side(data.getSide())
                .price(data.getPrice())
                .symbol(data.getSymbol())
                .paidWith(data.getPaidWith())
                .paidAmount(data.getAmount())
                .feeAmount(data.getFee())
                .created(LocalDateTime.now())
                .createdBy("BinanceSyncService")
                .portfolio(portfolio)
                .build();
    }
}
