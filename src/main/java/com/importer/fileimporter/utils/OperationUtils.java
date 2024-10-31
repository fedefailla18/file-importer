package com.importer.fileimporter.utils;

import com.importer.fileimporter.dto.TransactionData;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

@UtilityClass
public class OperationUtils {

    public static final List<String> SYMBOL = List.of("XVG", "BAND", "RSR", "AKRO", "DOT", "OP",
            "VET", "RLC", "BTC", "ETH");
    public static final List<String> GRAND_SYMBOLS = List.of("BTC", "ETH");
    public static final List<String> STABLE = List.of("USDT", "DAI", "BUSD", "UST", "USD", "USDC");

    public static final String BUY_STRING = "BUY";
    public static final Predicate<String> IS_BUY;
    public static final String SELL_STRING = "SELL";

    static {
        IS_BUY = BUY_STRING::equals;
    }

    public static final String USDT = "USDT";
    public static final String BTC = "BTC";

    public boolean isStable(String payedWithSymbol) {
        return STABLE.contains(payedWithSymbol);
    }

    public boolean isBuy(TransactionData transactionData) {
        String side = transactionData.getSide();
        return IS_BUY.test(side);
    }

    public boolean isBuy(String side) {
        return IS_BUY.test(side);
    }

    public BigDecimal sumAmount(AtomicReference<BigDecimal> amountSpent, BigDecimal payedAmount, String side) {
        return sumAmount(amountSpent.get(), payedAmount, side);
    }

    public BigDecimal sumAmount(BigDecimal currentAmount, BigDecimal paidAmount, String side) {
        currentAmount = getSafeValue(currentAmount);
        paidAmount = getSafeValue(paidAmount);
        return isBuy(side) ? currentAmount.add(paidAmount) : currentAmount.subtract(paidAmount);
    }

    public BigDecimal accumulateExecutedAmount(BigDecimal currentAmount, BigDecimal executed, String side) {
        return accumulateExecutedAmount(currentAmount, executed, isBuy(side));
    }

    public BigDecimal accumulateExecutedAmount(BigDecimal currentAmount, BigDecimal executed, Boolean isBuy) {
        return isBuy ?
                getSafeValue(currentAmount).add(getSafeValue(executed)) :
                getSafeValue(currentAmount).subtract(getSafeValue(executed));
    }

    public BigDecimal sumBigDecimal(BigDecimal bigDecimal1, BigDecimal bigDecimal2) {
        BigDecimal safeValue1 = getSafeValue(bigDecimal1);
        BigDecimal safeValue2 = getSafeValue(bigDecimal2);
        return safeValue1.add(safeValue2);
    }

    private BigDecimal getSafeValue(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    public static Optional<String> hasStable(String pair) {
        return STABLE.stream()
                .filter(pair::contains) // this is to catch the executed coin which should be at the beginning of the pair
                .findFirst();
    }
}
