package com.importer.fileimporter.controller;

import com.importer.fileimporter.dto.CoinInformationResponse;
import com.importer.fileimporter.dto.FileInformationResponse;
import com.importer.fileimporter.dto.TransactionDto;
import com.importer.fileimporter.dto.TransactionHoldingDto;
import com.importer.fileimporter.entity.Transaction;
import com.importer.fileimporter.entity.User;
import com.importer.fileimporter.facade.CoinInformationFacade;
import com.importer.fileimporter.service.ProcessFileFactory;
import com.importer.fileimporter.service.TransactionFacade;
import com.importer.fileimporter.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@Tag(name = "Transaction", description = "Transaction management APIs")
public class TransactionController {

    private final ProcessFileFactory processFileFactory;
    private final TransactionService transactionService;
    private final TransactionFacade transactionFacade;
    private final CoinInformationFacade coinInformationFacade;

    @Operation(summary = "Filter transactions", description = "Filter transactions by various criteria with pagination")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved transactions",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @GetMapping(value = "/filter")
    public Page<Transaction> getTransactionsRangeDate(
            @Parameter(description = "Symbol to filter by") @RequestParam(required = false) String symbol,
            @Parameter(description = "Portfolio name to filter by") @RequestParam(required = false) String portfolioName,
            @Parameter(description = "Transaction side (BUY/SELL)") @RequestParam(required = false) String side,
            @Parameter(description = "Currency paid with") @RequestParam(required = false) String paidWith,
            @Parameter(description = "Operator for paid amount (>, <, =, >=, <=)") @RequestParam(required = false) String paidAmountOperator,
            @Parameter(description = "Amount paid") @RequestParam(required = false) BigDecimal paidAmount,
            @Parameter(description = "Start date (ISO format)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (ISO format)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal User user,
            @Parameter(description = "Pagination parameters") @PageableDefault(sort = "dateUtc", direction = Sort.Direction.DESC) Pageable pageable) {
        return transactionFacade.filterTransactions(symbol, portfolioName, side, paidWith, paidAmountOperator, paidAmount,
                startDate, endDate, user.getId(), pageable);
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

    @Operation(summary = "Upload transaction file", description = "Upload and process a file containing transactions")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File successfully processed",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = FileInformationResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid file or format", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @PostMapping(value = "/upload")
    public FileInformationResponse uploadTransactions(
            @Parameter(description = "Transaction file to upload", required = true) @RequestBody MultipartFile file,
            @Parameter(description = "List of symbols to filter by") @RequestParam(required = false) List<String> symbols) throws IOException {
        if (file.isEmpty()) {
            return null;
        }
        return processFileFactory.processFile(file, symbols);
    }

    @Operation(summary = "Upload transaction file to portfolio", description = "Upload and process a file containing transactions and assign to a specific portfolio")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File successfully processed",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = FileInformationResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid file or format", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @PostMapping(value = "/upload/{portfolio}")
    public FileInformationResponse uploadTransactionsWithPortfolio(
            @Parameter(description = "Transaction file to upload", required = true) @RequestBody MultipartFile file,
            @Parameter(description = "List of symbols to filter by") @RequestParam(required = false) List<String> symbols,
            @Parameter(description = "Portfolio name", required = true) @PathVariable String portfolio) throws IOException {
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
        transactionFacade.deleteTransactions();
        return ResponseEntity.accepted();
    }

    @GetMapping
    @Deprecated
    public Page<Transaction> getAllTransactions(Pageable pageable) {
        return transactionService.getAll(pageable);
    }

    @Operation(summary = "Add a new transaction", description = "Manually add a new transaction to the system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Transaction successfully created",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Transaction.class))),
        @ApiResponse(responseCode = "400", description = "Invalid transaction data", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Transaction addTransaction(
            @Parameter(description = "Transaction data", required = true) @RequestBody TransactionDto request) {
        return transactionFacade.save(request);
    }

}
