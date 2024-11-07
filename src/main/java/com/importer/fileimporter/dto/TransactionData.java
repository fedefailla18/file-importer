package com.importer.fileimporter.dto;

import java.math.BigDecimal;

public interface TransactionData {

    String getDate();
    String getPair();
    String getSide();
    BigDecimal getPrice();
    BigDecimal getExecuted();
    BigDecimal getAmount();
    BigDecimal getFee();
    String getSymbol();
    String getFeeSymbol();
    String getCoinName();
    String getPaidWith();
}
