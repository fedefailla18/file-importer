package com.importer.fileimporter.service;

import com.importer.fileimporter.dto.CoinInformationResponse;
import com.importer.fileimporter.dto.FileInformationResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static java.math.BigDecimal.ZERO;

@AllArgsConstructor
@Service
@Slf4j
public class ProcessFile {

    public static final List<String> SYMBOL = List.of("WAVES");
//    public static final List<String> SYMBOL = List.of("XVG", "BAND", "RSR", "AKRO", "DOT", "OP", "WAVES",
//            "VET", "RLC", "BTC", "ETH");
    public static final List<String> GRAND_SYMBOLS = List.of("BTC", "ETH");
    public static final List<String> STABLE = List.of("USDT", "DAI", "BUSD", "UST", "USDC");

    public static final Predicate<String> IS_BUY = "BUY"::equals;
    public static final Predicate<String> IS_SELL = "SELL"::equals;

    private final FileImporterService fileImporterService;
    private final GetSymbolHistoricPriceService getSymbolHistoricPriceService;

    public FileInformationResponse processFile(MultipartFile file) throws IOException {
        return processFile(file, SYMBOL);
    }

    public FileInformationResponse processFile(MultipartFile file, List<String> symbols) throws IOException {
        if (symbols == null || symbols.isEmpty()) {
            symbols = SYMBOL;
        }
        List<Map<?, ?>> rows = fileImporterService.getRows(file);
//        Map<String, AtomicInteger> counter = getAmountOfTransactionsByCoin(rows);
        Map<String, CoinInformationResponse> transactionDetailInformation = getTransactionDetailInformation(rows, symbols);

        calculateAvgPrice(transactionDetailInformation);

        return FileInformationResponse.builder()
//                .rows(rows)
//                .each(counter)
                .amount(rows.size())
                .coinInformationResponse(transactionDetailInformation.values())
                .build();
    }

    private void calculateAvgPrice(Map<String, CoinInformationResponse> transactionDetailInformation) {

        transactionDetailInformation.values().stream()
                .parallel()
                .forEach(detail -> {

                    final var symbol = detail.getCoinName();

                    detail.getRows()
                            .forEach(row -> {
                                String pair = getPair(row);
                                String symbolPair = pair.replace(symbol, "");
                                boolean isBuy = isBuy(row);
                                var executed = getExecuted(row, symbol);
                                BigDecimal price = getPrice(row);

                                if (isStable(symbolPair).isPresent()) {

                                    detail.setAvgEntryPrice(symbolPair, price, executed, isBuy);
                                } else {
                                    log.info("No a Stable Coin transaction. " + symbolPair);
                                    String date = row.get("Date(UTC)").toString();

                                    BigDecimal priceInUsdt = getSymbolHistoricPriceService.getPriceInUsdt(symbolPair, price, date);
                                    // save transaction. store priceInUsdt so it's easier to calculate in the future
                                    if (0 <= priceInUsdt.doubleValue()) {
                                        detail.setAvgEntryPrice("USDT", priceInUsdt, executed, isBuy);
                                    } else {
                                        log.warn("Check transaction for date " + date);
                                    }
                                }
                            });
                    detail.calculateAvgPrice();
                });
    }

    private Map<String, CoinInformationResponse> getTransactionDetailInformation(List<Map<?, ?>> rows, List<String> symbols) {
        Map<String, CoinInformationResponse> informationResponseMap = new HashMap<>();

        rows.forEach(row -> {
            String pair = getPair(row);
            final var symbol = findTokenTransaction(pair, symbols);
            symbol.ifPresent(s -> {
                String symbolPair = pair.replace(s, "");
                boolean isBuy = isBuy(row);

                if (!informationResponseMap.containsKey(s)) {
                    informationResponseMap.computeIfAbsent(s, k -> {
                        var a = CoinInformationResponse.builder()
                                .amount(ZERO)
                                .usdSpent(ZERO)
                                .rows(new ArrayList<>())
                                .coinName(k)
                                .spent(new HashMap<>())
                                .avgEntryPrice(new HashMap<>())
                                .build();
                        a.setAmount(calculateAmount(a.getAmount(), isBuy, getExecuted(row, s)));

                        Optional<String> first = STABLE.stream().filter(symbolPair::contains).findFirst();
                        first.ifPresent(value ->
                                a.setUsdSpent(getAmountSpent(a.getUsdSpent(), getAmount(row, value), isBuy)));

                        calculateSpent(getAmount(row, symbolPair), a, symbolPair, isBuy);

                        a.addRows(row);
                        return a;
                    });
                    return;
                }
                CoinInformationResponse a = informationResponseMap.get(s);
                a.setAmount(calculateAmount(a.getAmount(), isBuy, getExecuted(row, s)));

                Optional<String> first = STABLE.stream().filter(symbolPair::contains)
                        .findFirst();
                if (first.isPresent()) {
                    a.setUsdSpent(getAmountSpent(a.getUsdSpent(), getAmount(row, first.get()), isBuy));
                }

                calculateSpent(getAmount(row, symbolPair), a, symbolPair, isBuy);
                a.addRows(row);
            });
        });
        return informationResponseMap;
    }

    private Optional<String> findTokenTransaction(String pair, List<String> symbols) {
        return symbols.stream()
                .filter(pair.substring(0,4)::contains) // this is to catch the executed coin which should be at the beginning of the pair
                .findFirst();
    }

    private Optional<String> isStable(String pair) {
        return STABLE.stream()
                .filter(pair::contains) // this is to catch the executed coin which should be at the beginning of the pair
                .findFirst();
    }

    private String getPair(Map<?, ?> row) {
        return row.get("Pair").toString();
    }

    private void calculateSpent(BigDecimal amount, CoinInformationResponse a, String symbolPair, boolean isBuy) {
        a.getSpent().computeIfAbsent(symbolPair, k -> new BigDecimal(0));
        if (isBuy) {
            a.getSpent().computeIfPresent(symbolPair, (k, v) -> v.add(amount));
        } else if (!isBuy){
            a.getSpent().computeIfPresent(symbolPair, (k, v) -> v.subtract(amount));
        }
    }

    private BigDecimal getAmountSpent(BigDecimal usdSpent, BigDecimal amount, boolean isBuy) {
        if (isBuy) {
            return usdSpent.add(amount);
        } else {
            return usdSpent.subtract(amount);
        }
    }

    private BigDecimal calculateAmount(BigDecimal aAmount, boolean isBuy, BigDecimal amountAdded) {
        if (isBuy) {
            return aAmount.add(amountAdded);
        } else {
            return aAmount.subtract(amountAdded);
        }
    }

    private Map<String, AtomicInteger> getAmountOfTransactionsByCoin(List<Map<?, ?>> rows) {
        Map<String, AtomicInteger> counter = new HashMap<>();
        countCoinPairTransactions(SYMBOL.get(0), rows, counter);
        countCoinPairTransactions(SYMBOL.get(1), rows, counter);
        countCoinPairTransactions(SYMBOL.get(2), rows, counter);
        return counter;
    }

    private void countCoinPairTransactions(String symbol, List<Map<?, ?>> rows, Map<String, AtomicInteger> counter) {
        rows.forEach(e -> {
            String pair = e.get("Pair").toString();
            boolean contains = pair.contains(symbol);
            if (contains) {
                counter.computeIfAbsent(symbol, k -> new AtomicInteger(0)).incrementAndGet();
            }
        });
    }

    private boolean isBuy(Map<?, ?> row) {
        String side = row.get("Side").toString();
        return IS_BUY.test(side);
    }

    private BigDecimal getExecuted(Map<?, ?> row, String coinName) {
        String executed = row.get("Executed").toString()
                .replace(coinName, "")
                .replace(",", "");
        double added = Double.parseDouble(executed);
        return BigDecimal.valueOf(added);
    }

    private BigDecimal getAmount(Map<?, ?> row, String symbolPair) {
        String amount = row.get("Amount").toString()
                .replace(symbolPair, "")
                .replace(",", "");
        return getBigDecimalWithScale(Double.valueOf(amount));
    }

    private BigDecimal getPrice(Map<?, ?> row) {
        String price = row.get("Price").toString()
                .replace(",", "");
        return getBigDecimalWithScale(Double.valueOf(price));
    }

    private BigDecimal getBigDecimalWithScale(Number number) {
        return BigDecimal.valueOf((Double) number).setScale(7, RoundingMode.UP);
    }
}
