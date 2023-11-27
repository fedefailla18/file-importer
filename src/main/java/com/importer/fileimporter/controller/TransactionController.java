package com.importer.fileimporter.controller;

import com.importer.fileimporter.entity.Transaction;
import com.importer.fileimporter.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/transaction")
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public List<Transaction> getTransactions(@RequestParam(required = false) String symbol) {
        return transactionService.getTransactions(symbol);
    }

}