package com.importer.fileimporter.service;

import com.importer.fileimporter.dto.CoinInformationResponse;
import com.importer.fileimporter.dto.FileInformationResponse;
import com.importer.fileimporter.utils.OperationUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static com.importer.fileimporter.utils.OperationUtils.STABLE;
import static com.importer.fileimporter.utils.OperationUtils.SYMBOL;
import static java.math.BigDecimal.ZERO;

@AllArgsConstructor
@Service
@Slf4j
public class ProcessFile {

//    public static final List<String> SYMBOL = List.of("WAVES");

    private final FileImporterService fileImporterService;
    private final TransactionService transactionService;
    private final CoinInformationService coinInformationService;

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

        BigDecimal bigDecimal = calculateAvgPrice(transactionDetailInformation);

        return FileInformationResponse.builder()
//                .rows(rows)
//                .each(counter)
                .amount(rows.size())
                .totalSpent(bigDecimal)
                .coinInformationResponse(transactionDetailInformation.values())
                .build();
    }

    private BigDecimal calculateAvgPrice(Map<String, CoinInformationResponse> transactionDetailInformation) {
        BigDecimal totalSpent = ZERO;

        transactionDetailInformation.values().stream()
                .parallel()
                .forEach(detail -> {

                    final var symbol = detail.getCoinName();

                    detail.getRows()
                            .forEach(row -> {
                                String pair = getPair(row);
                                String symbolPair = pair.replace(symbol, "");
                                boolean isBuy = OperationUtils.isBuy(row);
                                var executed = getExecuted(row, symbol);
                                BigDecimal price = getPrice(row);

                                coinInformationService.calculateAvgEntryPrice(detail, row.get("Date(UTC)").toString(), symbolPair, isBuy, executed, price);
                            });
                    detail.calculateAvgPrice();
                    totalSpent.add(detail.getTotalStable());
                });
        return totalSpent;
    }

    private Map<String, CoinInformationResponse> getTransactionDetailInformation(List<Map<?, ?>> rows, List<String> symbols) {
        Map<String, CoinInformationResponse> transactionsDetailsMap = new HashMap<>();

        rows.forEach(processRow(symbols, transactionsDetailsMap));
        return transactionsDetailsMap;
    }

    private Consumer<Map<?, ?>> processRow(List<String> symbols,
                                           Map<String, CoinInformationResponse> transactionsDetailsMap) {
        return row -> {
            String pair = getPair(row);
            final var symbol = findTokenTransaction(pair, symbols);
            symbol.ifPresent(s -> {
                String symbolPair = pair.replace(s, "");
                boolean isBuy = OperationUtils.isBuy(row);

                if (!transactionsDetailsMap.containsKey(s)) {
                    transactionsDetailsMap.computeIfAbsent(s, k -> {
                        var a = CoinInformationResponse.builder()
                                .amount(ZERO)
                                .usdSpent(ZERO)
                                .rows(new ArrayList<>())
                                .coinName(k)
                                .spent(new HashMap<>())
                                .avgEntryPrice(new HashMap<>())
                                .build();
                        a.setAmount(calculateAmount(a.getAmount(), isBuy, getExecuted(row, s)));

                        // TODO:
                        Optional<String> first = STABLE.stream().filter(symbolPair::contains).findFirst();
                        first.ifPresent(value ->
                                a.setUsdSpent(getAmountSpent(a.getUsdSpent(), getAmount(row, value), isBuy)));

                        calculateSpent(getAmount(row, symbolPair), a, symbolPair, isBuy);

                        a.addRows(row);

                        transactionService.saveTransaction(a.getCoinName(), symbolPair, row.get("Date(UTC)").toString(), pair,
                                row.get("Side").toString(), getPrice(row), getExecuted(row, a.getCoinName()),
                                getAmount(row, a.getCoinName()), getFee(row), "Process File - New Coin");
                        return a;
                    });
                    return;
                }
                CoinInformationResponse a = transactionsDetailsMap.get(s);
                a.setAmount(calculateAmount(a.getAmount(), isBuy, getExecuted(row, s)));

                Optional<String> first = STABLE.stream().filter(symbolPair::contains)
                        .findFirst();
                first.ifPresent(value ->
                        a.setUsdSpent(getAmountSpent(a.getUsdSpent(), getAmount(row, value), isBuy)));

                calculateSpent(getAmount(row, symbolPair), a, symbolPair, isBuy);
                a.addRows(row);
                transactionService.saveTransaction(a.getCoinName(), symbolPair, row.get("Date(UTC)").toString(), pair, row.get("Side").toString(), getPrice(row),
                        getExecuted(row, a.getCoinName()), getAmount(row, a.getCoinName()), getFee(row), "Process File");
            });
        };
    }

    private Optional<String> findTokenTransaction(String pair, List<String> symbols) {
        return symbols.stream()
                .filter(pair.substring(0,4)::contains) // this is to catch the executed coin which should be at the beginning of the pair
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

    private BigDecimal getExecuted(Map<?, ?> row, String coinName) {
        String executed = row.get("Executed").toString()
                .replace(coinName, "")
                .replace(",", "");
        double added = Double.parseDouble(executed);
        return BigDecimal.valueOf(added);
    }

    private BigDecimal getAmount(Map<?, ?> row, String symbolPair) {
        String amount1 = row.get("Amount").toString();
        String amount = amount1
                .substring(0, amount1.length()-6)
                .replace(",", "");
        return getBigDecimalWithScale(Double.valueOf(amount));
    }

    // TODO: retrieve de symbol that affected the fee
    private BigDecimal getFee(Map<?, ?> row) {
        String feeString = row.get("Fee").toString();
        String fee = feeString.substring(0, feeString.length()-6);
        return getBigDecimalWithScale(Double.valueOf(fee));
    }
    private String getFeeSymbol(String feeString, String symbol) {
        Optional<String> first = Arrays.asList("BNB", "USDT", "BTC").stream()
                .filter(e -> feeString.contains(e))
                .findFirst();

        String fee2 = feeString.replace(first.get(), "");
        return first.get();
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
