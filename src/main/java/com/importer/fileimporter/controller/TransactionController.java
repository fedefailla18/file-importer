package com.importer.fileimporter.controller;

import com.importer.fileimporter.dto.FileInformationResponse;
import com.importer.fileimporter.entity.Transaction;
import com.importer.fileimporter.service.ProcessFile;
import com.importer.fileimporter.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/transaction")
@Slf4j
public class TransactionController {

    private final ProcessFile processFile;
    private final TransactionService transactionService;

    @GetMapping
    public Page<Transaction> getTransactions(@RequestParam(required = false) String symbol, @PageableDefault Pageable pageable) {
        return transactionService.getTransactions(symbol, pageable);
    }

    @PostMapping(value = "/upload")
    public FileInformationResponse uploadTransactions(@RequestBody MultipartFile file,
                                                      @RequestParam(required = false) List<String> symbols) throws IOException {
        if (file.isEmpty()) {
            return null;
        }
        return processFile.processFile(file, symbols);
    }

}