package com.importer.fileimporter.controller;

import com.importer.fileimporter.dto.CoinInformationResponse;
import com.importer.fileimporter.dto.FileInformationResponse;
import com.importer.fileimporter.dto.TransactionHoldingDto;
import com.importer.fileimporter.entity.Transaction;
import com.importer.fileimporter.service.ProcessFile;
import com.importer.fileimporter.service.TransactionFacade;
import com.importer.fileimporter.service.TransactionService;
import com.importer.fileimporter.service.usecase.CalculateAmountSpent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    private final ProcessFile processFile;
    private final TransactionService transactionService;
    private final TransactionFacade transactionFacade;
    private final CalculateAmountSpent calculateAmountSpent;

    @GetMapping(value = "/filter")
    public Page<Transaction> getTransactionsRangeDate(@RequestParam(required = true) String symbol,
                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                      @PageableDefault Pageable pageable) {
        return transactionService.getTransactionsByRangeDate(symbol, startDate, endDate, pageable);
    }

    @GetMapping("/information")
    public CoinInformationResponse getSymbolInformation(@RequestParam(required = false) String symbol,
                                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                              @PageableDefault Pageable pageable) {
        return transactionService.getTransactionsInformation(symbol, startDate, endDate, pageable);
    }

    @GetMapping("/spentAmount")
    public BigDecimal getAmountSpent(@RequestParam(required = false) String symbol,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                     @PageableDefault Pageable pageable) {
        return calculateAmountSpent.execute(symbol, startDate, endDate, pageable);
    }

    @PostMapping(value = "/upload")
    public FileInformationResponse uploadTransactions(@RequestBody MultipartFile file,
                                                      @RequestParam(required = false) List<String> symbols) throws IOException {
        if (file.isEmpty()) {
            return null;
        }
        return processFile.processFile(file, symbols);
    }

    @GetMapping(value = "/portfolio")
    public List<TransactionHoldingDto> createPortfolio(@RequestParam(required = false) List<String> symbols) throws IOException {
        return transactionFacade.buildPortfolio(symbols);
    }

    @GetMapping(value = "/portfolio/amount")
    public List<TransactionHoldingDto> getAmount(@RequestParam(required = false) List<String> symbols) throws IOException {
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

}