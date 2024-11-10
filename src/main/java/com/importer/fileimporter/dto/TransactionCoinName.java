package com.importer.fileimporter.dto;

public abstract class TransactionCoinName implements TransactionData {

    protected String coinName;

    protected TransactionCoinName() {
    }

    public String getCoinName() {
        return this.coinName;
    }

}
