package com.importer.fileimporter.service;

import com.importer.fileimporter.dto.BinanceTransactionAdapter;
import com.importer.fileimporter.dto.MexcTransactionAdapter;
import com.importer.fileimporter.dto.TransactionData;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TransactionAdapterFactory {
    public TransactionData createAdapter(Map<?, ?> row, String fileType) {
        switch (fileType.toUpperCase()) {
            case "BINANCE":
                return new BinanceTransactionAdapter(row);
            case "MEXC":
                return new MexcTransactionAdapter(row);
            default:
                throw new IllegalArgumentException("Unsupported file type: " + fileType);
        }
    }
}
