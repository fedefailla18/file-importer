package com.importer.fileimporter.facade;

import com.importer.fileimporter.dto.CoinInformationResponse;
import com.importer.fileimporter.entity.Portfolio;
import com.importer.fileimporter.entity.Transaction;
import com.importer.fileimporter.service.CoinInformationService;
import com.importer.fileimporter.service.PortfolioService;
import com.importer.fileimporter.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class CoinInformationFacade {

    private final TransactionService transactionService;
    private final PortfolioService portfolioService;
    private final CoinInformationService coinInformationService;

    public List<CoinInformationResponse> getTransactionsInformation() {
        return transactionService.getAll().stream()
                .collect(Collectors.groupingByConcurrent(Transaction::getSymbol))
                .entrySet().stream()
                .map(this::getCoinInformationResponse)
                .collect(Collectors.toList());
    }

    public List<CoinInformationResponse> getPortfolioTransactionsInformation(String portfolio) {
        Portfolio byName = portfolioService.getByName(portfolio)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Portfolio not found."));
        List<Transaction> transactions = transactionService.findByPortfolio(byName);
        Map<String, List<Transaction>> collect = transactions.stream()
                .collect(Collectors.groupingBy(Transaction::getSymbol));
        return collect.entrySet().stream()
                .map(this::getCoinInformationResponse)
                .collect(Collectors.toList());
    }

    public CoinInformationResponse getTransactionsInformationBySymbol(String symbol) {
        List<Transaction> transactions = transactionService.getAllBySymbol(symbol);
        return getCoinInformationResponse(symbol, transactions);
    }

    private CoinInformationResponse getCoinInformationResponse(Map.Entry<String, List<Transaction>> entrySet) {
        return getCoinInformationResponse(entrySet.getKey(), entrySet.getValue());
    }

    private CoinInformationResponse getCoinInformationResponse(String symbol, List<Transaction> transactions) {
        return coinInformationService.getCoinInformationResponse(symbol, transactions);
    }

}
