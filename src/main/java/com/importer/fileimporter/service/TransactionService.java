package com.importer.fileimporter.service;

import com.importer.fileimporter.entity.Transaction;
import com.importer.fileimporter.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public List<Transaction> getTransactions(String symbol) {
        if (StringUtils.hasText(symbol)) {
            return transactionRepository.findAllBySymbol(symbol);
        } else {
            return transactionRepository.findAll();
        }
    }
}
