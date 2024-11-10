package com.importer.fileimporter.dto;
import com.importer.fileimporter.utils.ProcessFileUtils;

import java.math.BigDecimal;
import java.util.Map;

public class BinanceTransactionAdapter extends TransactionCoinName {

    public static final String PAIR_KEY = "Pair";
    public static final String EXECUTED_KEY = "Executed";
    public static final String AMOUNT_KEY = "Amount";
    public static final String FEE_KEY = "Fee";
    public static final String PRICE_KEY = "Price";
    public static final String DATE_KEY = "Date(UTC)";
    public static final String SIDE_KEY = "Side";

    private final Map<?, ?> row;

    public BinanceTransactionAdapter(Map<?, ?> row) {
        this.row = row;
        this.coinName = getSymbol();
    }

    @Override
    public String getDate() {
        return row.get(DATE_KEY).toString();
    }

    @Override
    public String getPair() {
        return row.get(PAIR_KEY).toString();
    }

    @Override
    public String getSide() {
        return row.get(SIDE_KEY).toString();
    }

    @Override
    public BigDecimal getPrice() {
        String price = row.get(PRICE_KEY).toString()
                .replace(",", "");
        return ProcessFileUtils.getBigDecimalWithScale(Double.valueOf(price));
    }

    @Override
    public BigDecimal getExecuted() {
        String executed = row.get(EXECUTED_KEY).toString()
                .replace(coinName, "")
                .replace(",", "");
        double added = Double.parseDouble(executed);
        return BigDecimal.valueOf(added);
    }

    @Override
    public BigDecimal getAmount() {
        String amount1 = row.get(AMOUNT_KEY).toString();
        String amount = amount1
                .substring(0, amount1.length() - coinName.length())
                .replace(",", "");
        return ProcessFileUtils.getBigDecimalWithScale(Double.valueOf(amount));
    }

    @Override
    public BigDecimal getFee() {
        String feeString = row.get(FEE_KEY).toString();
        String fee = feeString.substring(0, feeString.length() - 6);
        return ProcessFileUtils.getBigDecimalWithScale(Double.valueOf(fee));
    }

    @Override
    public String getSymbol() {
        String executedString = row.get(EXECUTED_KEY).toString();
        return ProcessFileUtils.getSymbolFromNumber(executedString);
    }

    @Override
    public String getFeeSymbol() {
        String feeString = row.get(FEE_KEY).toString();
        String feeSymbol = ProcessFileUtils.getSymbolFromNumber(feeString);

        if (feeSymbol.matches("\\s*")) {
            throw new IllegalArgumentException("No symbol found in fee string");
        }

        return feeSymbol.trim();
    }

    @Override
    public String getPaidWith() {
        return getPair().replace(getCoinName(), "");
    }

}
