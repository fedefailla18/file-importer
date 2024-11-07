package com.importer.fileimporter.controller;

import com.importer.fileimporter.dto.CoinInformationResponse;
import com.importer.fileimporter.dto.FileInformationResponse;
import com.importer.fileimporter.dto.TransactionDto;
import com.importer.fileimporter.dto.TransactionHoldingDto;
import com.importer.fileimporter.entity.Transaction;
import com.importer.fileimporter.facade.CoinInformationFacade;
import com.importer.fileimporter.service.ProcessFileFactory;
import com.importer.fileimporter.service.TransactionFacade;
import com.importer.fileimporter.service.TransactionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/transaction")
@Slf4j
public class TransactionController {

    private final ProcessFileFactory processFileFactory;
    private final TransactionService transactionService;
    private final TransactionFacade transactionFacade;
    private final CoinInformationFacade coinInformationFacade;

    @GetMapping(value = "/filter")
    public Page<Transaction> getTransactionsRangeDate(@RequestParam(required = false) String symbol,
                                                      @RequestParam(required = false) String portfolioName,
                                                      @RequestParam(required = false) String side,
                                                      @RequestParam(required = false) String paidWith,
                                                      @RequestParam(required = false) String paidAmountOperator,
                                                      @RequestParam(required = false) BigDecimal paidAmount,
                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                      @PageableDefault(sort = "dateUtc", direction = Sort.Direction.DESC) Pageable pageable) {
        return transactionService.filterTransactions(symbol, portfolioName, side, paidWith, paidAmountOperator, paidAmount, startDate, endDate, pageable);
    }

    @PostMapping("/information")
    public CoinInformationResponse getSymbolInformation(@RequestParam String symbol) {
        return coinInformationFacade.getTransactionsInformationBySymbol(symbol);
    }

    @PostMapping("/information/all")
    public List<CoinInformationResponse> getInformation() {
        return coinInformationFacade.getTransactionsInformation();
    }

    @Tag(name = "/information/all/{portfolio}", description = "Calculates portfolio stats from transactions")
    @PostMapping("/information/all/{portfolio}")
    public List<CoinInformationResponse> getInformation(@PathVariable String portfolio) {
        return coinInformationFacade.getPortfolioTransactionsInformation(portfolio);
    }

    @PostMapping(value = "/upload")
    public FileInformationResponse uploadTransactions(@RequestBody MultipartFile file,
                                                      @RequestParam(required = false) List<String> symbols) throws IOException {
        if (file.isEmpty()) {
            return null;
        }
        return processFileFactory.processFile(file, symbols);
    }

    @PostMapping(value = "/upload/{portfolio}")
    public FileInformationResponse uploadTransactionsWithPortfolio(@RequestBody MultipartFile file,
                                                                   @RequestParam(required = false) List<String> symbols,
                                                                   @PathVariable String portfolio) throws IOException {
        if (file.isEmpty()) {
            return null;
        }
        return processFileFactory.processFile(file, symbols, portfolio);
    }

    @GetMapping(value = "/portfolio")
    public List<TransactionHoldingDto> createPortfolio(@RequestParam(required = false) List<String> symbols) {
        return transactionFacade.buildPortfolio(symbols);
    }

    @GetMapping(value = "/portfolio/amount")
    public List<TransactionHoldingDto> getAmount(@RequestParam(required = false) List<String> symbols) {
        return transactionFacade.getAmount(symbols);
    }

    @DeleteMapping
    public ResponseEntity.BodyBuilder deleteTransactions() {
        transactionService.deleteTransactions();
        return ResponseEntity.accepted();
    }

    @GetMapping
    public Page<Transaction> getAllTransactions(Pageable pageable) {
        return transactionService.getAll(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Transaction addTransaction(@RequestBody TransactionDto request) {
        return transactionFacade.save(request);
    }

}
