package com.importer.fileimporter.service;

import com.importer.fileimporter.dto.BinanceApiTransactionAdapter;
import com.importer.fileimporter.dto.integration.binance.*;
import com.importer.fileimporter.entity.*;
import com.importer.fileimporter.repository.UserExchangeConfigRepository;
import com.importer.fileimporter.utils.DateUtils;
import com.importer.fileimporter.utils.OperationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BinanceFullSyncService {

    private final BinanceApiService binanceApiService;
    private final UserExchangeConfigRepository userExchangeConfigRepository;
    private final EncryptionService encryptionService;
    private final TransactionProcessor transactionProcessor;
    private final PortfolioService portfolioService;

    private static final long WINDOW_90_DAYS = 90L * 24 * 60 * 60 * 1000;
    private static final long WINDOW_30_DAYS = 30L * 24 * 60 * 60 * 1000;
    private static final long START_TIME_2017 = 1483228800000L; // 2017-01-01

    @Transactional
    public void syncFullHistory(User user, String portfolioName) {
        syncFullHistory(user, portfolioName, START_TIME_2017, System.currentTimeMillis());
    }

    @Transactional
    public void syncFullHistory(User user, String portfolioName, Long startDateEpochMs, Long endDateEpochMs) {
        UserExchangeConfig config = userExchangeConfigRepository.findByUserAndExchangeName(user, ExchangeName.BINANCE)
                .orElseThrow(() -> new IllegalArgumentException("Binance API keys not configured for user"));

        String apiKey = config.getApiKey();
        String secretKey = encryptionService.decrypt(config.getApiSecret());
        Portfolio portfolio = portfolioService.findOrSave(portfolioName);

        long now = System.currentTimeMillis();
        long startTime = startDateEpochMs != null ? startDateEpochMs : START_TIME_2017;
        long endTime = endDateEpochMs != null ? endDateEpochMs : now;

        // 1. Sync Deposits
        syncDeposits(apiKey, secretKey, portfolio, startTime, endTime);

        // 2. Sync Withdrawals
        syncWithdrawals(apiKey, secretKey, portfolio, startTime, endTime);

        // 3. Sync Fiat Orders
        syncFiatOrders(apiKey, secretKey, portfolio, startTime, endTime);

        // 4. Sync Convert Trades
        syncConvertTrades(apiKey, secretKey, portfolio, startTime, endTime);

        // 5. Sync Spot Trades (using fromId paging)
        syncSpotTrades(apiKey, secretKey, portfolio, startTime, endTime);

        config.setLastSyncTimestamp(now);
        userExchangeConfigRepository.save(config);
    }

    private void syncDeposits(String apiKey, String secretKey, Portfolio portfolio, long start, long end) {
        log.info("Syncing Binance Deposits...");
        for (long t = start; t < end; t += WINDOW_90_DAYS) {
            long e = Math.min(t + WINDOW_90_DAYS, end);
            try {
                List<BinanceDepositResponse> deposits = binanceApiService.getDepositHistory(apiKey, secretKey, t, e);
                if (deposits != null) {
                    for (BinanceDepositResponse d : deposits) {
                        Transaction tx = Transaction.builder()
                                .dateUtc(toLocalDateTime(d.getInsertTime()))
                                .side(OperationUtils.DEPOSIT_STRING)
                                .symbol(d.getCoin())
                                .executed(d.getAmount())
                                .price(BigDecimal.ZERO)
                                .pair(d.getCoin() + "EXTERNAL")
                                .created(LocalDateTime.now())
                                .createdBy("BinanceFullSync-Deposit")
                                .portfolio(portfolio)
                                .build();
                        transactionProcessor.process(tx);
                    }
                }
            } catch (Exception ex) {
                log.error("Error syncing deposits for window {} - {}: {}", t, e, ex.getMessage());
            }
            sleep(500);
        }
    }

    private void syncWithdrawals(String apiKey, String secretKey, Portfolio portfolio, long start, long end) {
        log.info("Syncing Binance Withdrawals...");
        for (long t = start; t < end; t += WINDOW_90_DAYS) {
            long e = Math.min(t + WINDOW_90_DAYS, end);
            try {
                List<BinanceWithdrawResponse> withdrawals = binanceApiService.getWithdrawHistory(apiKey, secretKey, t, e);
                if (withdrawals != null) {
                    for (BinanceWithdrawResponse w : withdrawals) {
                        Transaction tx = Transaction.builder()
                                .dateUtc(parseDateTime(w.getApplyTime()))
                                .side(OperationUtils.WITHDRAW_STRING)
                                .symbol(w.getCoin())
                                .executed(w.getAmount())
                                .price(BigDecimal.ZERO)
                                .pair(w.getCoin() + "EXTERNAL")
                                .feeAmount(w.getTransactionFee())
                                .feeSymbol(w.getCoin())
                                .created(LocalDateTime.now())
                                .createdBy("BinanceFullSync-Withdraw")
                                .portfolio(portfolio)
                                .build();
                        transactionProcessor.process(tx);
                    }
                }
            } catch (Exception ex) {
                log.error("Error syncing withdrawals for window {} - {}: {}", t, e, ex.getMessage());
            }
            sleep(500);
        }
    }

    private void syncFiatOrders(String apiKey, String secretKey, Portfolio portfolio, long start, long end) {
        log.info("Syncing Binance Fiat Orders...");
        for (int type : new int[]{0, 1}) {
            for (long t = start; t < end; t += WINDOW_90_DAYS) {
                long e = Math.min(t + WINDOW_90_DAYS, end);
                try {
                    BinanceFiatOrderResponse resp = binanceApiService.getFiatOrders(apiKey, secretKey, type, t, e);
                    if (resp != null && resp.getData() != null) {
                        for (BinanceFiatOrderResponse.FiatOrder o : resp.getData()) {
                            if (!"Completed".equalsIgnoreCase(o.getStatus())) continue;
                            
                            Transaction tx = Transaction.builder()
                                    .dateUtc(toLocalDateTime(o.getCreateTime()))
                                    .side(type == 0 ? OperationUtils.DEPOSIT_STRING : OperationUtils.WITHDRAW_STRING)
                                    .symbol(o.getCryptoCurrency())
                                    .executed(new BigDecimal(o.getObtainAmount()))
                                    .price(new BigDecimal(o.getPrice()))
                                    .paidWith(o.getFiatCurrency())
                                    .paidAmount(new BigDecimal(o.getSourceAmount()))
                                    .pair(o.getCryptoCurrency() + o.getFiatCurrency())
                                    .feeAmount(o.getTotalFee())
                                    .feeSymbol(o.getFiatCurrency())
                                    .created(LocalDateTime.now())
                                    .createdBy("BinanceFullSync-FiatOrder")
                                    .portfolio(portfolio)
                                    .build();
                            transactionProcessor.process(tx);
                        }
                    }
                } catch (Exception ex) {
                    log.error("Error syncing fiat orders type {} window {} - {}: {}", type, t, e, ex.getMessage());
                }
                sleep(500);
            }
        }
    }

    private void syncConvertTrades(String apiKey, String secretKey, Portfolio portfolio, long start, long end) {
        log.info("Syncing Binance Convert Trades...");
        for (long t = start; t < end; t += WINDOW_30_DAYS) {
            long e = Math.min(t + WINDOW_30_DAYS, end);
            try {
                BinanceConvertTradeResponse resp = binanceApiService.getConvertTradeHistory(apiKey, secretKey, t, e);
                if (resp != null && resp.getList() != null) {
                    for (BinanceConvertTradeResponse.ConvertTrade c : resp.getList()) {
                        if (!"SUCCESS".equalsIgnoreCase(c.getOrderStatus())) continue;

                        Transaction tx = Transaction.builder()
                                .dateUtc(toLocalDateTime(c.getCreateTime()))
                                .side(OperationUtils.BUY_STRING)
                                .symbol(c.getToAsset())
                                .executed(c.getToAmount())
                                .price(c.getRatio())
                                .paidWith(c.getFromAsset())
                                .paidAmount(c.getFromAmount())
                                .pair(c.getToAsset() + c.getFromAsset())
                                .created(LocalDateTime.now())
                                .createdBy("BinanceFullSync-Convert")
                                .portfolio(portfolio)
                                .build();
                        transactionProcessor.process(tx);
                    }
                }
            } catch (Exception ex) {
                log.error("Error syncing convert trades window {} - {}: {}", t, e, ex.getMessage());
            }
            sleep(500);
        }
    }

    private void syncSpotTrades(String apiKey, String secretKey, Portfolio portfolio, long start, long end) {
        log.info("Syncing Binance Spot Trades...");
        
        BinanceAccountResponse account = binanceApiService.getAccountInfo(apiKey, secretKey);
        Set<String> assets = account.getBalances().stream()
                .filter(b -> b.getFree().add(b.getLocked()).compareTo(BigDecimal.ZERO) > 0)
                .map(BinanceAccountResponse.AssetBalance::getAsset)
                .collect(Collectors.toSet());

        BinanceExchangeInfoResponse exchangeInfo = binanceApiService.getExchangeInfo();
        List<BinanceExchangeInfoResponse.SymbolInfo> relevantSymbols = exchangeInfo.getSymbols().stream()
                .filter(s -> assets.contains(s.getBaseAsset()) || assets.contains(s.getQuoteAsset()))
                .collect(Collectors.toList());

        for (BinanceExchangeInfoResponse.SymbolInfo sInfo : relevantSymbols) {
            log.info("Fetching exhaustive history for {}", sInfo.getSymbol());
            
            Long lastTradeId = null;
            boolean hasMore = true;
            
            while (hasMore) {
                try {
                    List<BinanceTradeResponse> trades;
                    if (lastTradeId == null) {
                        trades = binanceApiService.getMyTrades(apiKey, secretKey, sInfo.getSymbol(), start, null, null);
                    } else {
                        trades = binanceApiService.getMyTrades(apiKey, secretKey, sInfo.getSymbol(), null, null, lastTradeId + 1);
                    }

                    if (trades == null || trades.isEmpty()) {
                        hasMore = false;
                    } else {
                        log.info("Processing {} trades for {}", trades.size(), sInfo.getSymbol());
                        for (BinanceTradeResponse tr : trades) {
                            BinanceApiTransactionAdapter adapter = new BinanceApiTransactionAdapter(tr, sInfo.getBaseAsset(), sInfo.getQuoteAsset());
                            Transaction tx = Transaction.builder()
                                    .dateUtc(DateUtils.getLocalDateTime(adapter.getDate()))
                                    .pair(adapter.getPair())
                                    .executed(adapter.getExecuted())
                                    .side(adapter.getSide())
                                    .price(adapter.getPrice())
                                    .symbol(adapter.getSymbol())
                                    .paidWith(adapter.getPaidWith())
                                    .paidAmount(adapter.getAmount())
                                    .feeAmount(adapter.getFee())
                                    .feeSymbol(adapter.getFeeSymbol())
                                    .created(LocalDateTime.now())
                                    .createdBy("BinanceFullSync-Spot")
                                    .portfolio(portfolio)
                                    .build();
                            transactionProcessor.process(tx);
                            lastTradeId = tr.getId();
                        }
                        if (trades.size() < 1000) hasMore = false;
                    }
                } catch (Exception ex) {
                    if (ex.getMessage() != null && ex.getMessage().contains("-1121")) {
                        log.debug("Symbol {} no longer exists or invalid", sInfo.getSymbol());
                    } else {
                        log.error("Error syncing spot {}: {}", sInfo.getSymbol(), ex.getMessage());
                    }
                    hasMore = false;
                }
                sleep(200);
            }
        }
    }

    private LocalDateTime toLocalDateTime(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.of("UTC"));
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.parse(dateTimeStr, formatter);
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
