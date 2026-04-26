package com.importer.fileimporter.service;

import com.google.common.base.Strings;
import com.importer.fileimporter.converter.TransactionConverter;
import com.importer.fileimporter.dto.HoldingDto;
import com.importer.fileimporter.dto.TransactionDto;
import com.importer.fileimporter.dto.TransactionHoldingDto;
import com.importer.fileimporter.entity.Holding;
import com.importer.fileimporter.entity.Portfolio;
import com.importer.fileimporter.entity.Transaction;
import com.importer.fileimporter.facade.PricingFacade;
import com.importer.fileimporter.utils.OperationUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.transaction.NotSupportedException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.importer.fileimporter.utils.OperationUtils.BTC;
import static com.importer.fileimporter.utils.OperationUtils.USDT;

@RequiredArgsConstructor
@Service
@Slf4j
public class TransactionFacade {

    private final TransactionService transactionService;
    private final PricingFacade pricingFacade;
    private final TransactionProcessor transactionProcessor;
    private final HoldingService holdingService;
    private final PortfolioService portfolioService;

    public List<TransactionHoldingDto> buildPortfolio(List<String> symbols) {
        return getAmount(symbols);
    }

    @SneakyThrows
    public List<TransactionHoldingDto> getAmount(List<String> symbols) {
        List<TransactionHoldingDto> holdingDtos = new ArrayList<>();
        if (CollectionUtils.isEmpty(symbols)) {
            // Fetch all portfolios and their holdings
            List<Portfolio> portfolios = portfolioService.findAll();
            for (Portfolio portfolio : portfolios) {
                List<Holding> holdings = holdingService.getByPortfolio(portfolio);
                holdingDtos.addAll(holdings.stream()
                        .map(this::mapToTransactionHoldingDto)
                        .collect(Collectors.toList()));
            }
        } else {
            // For now, if symbols are provided, we search in all portfolios
            for (String symbol : symbols) {
                List<HoldingDto> holdingsBySymbol = holdingService.getBySymbol(symbol.toUpperCase());
                for (HoldingDto holdingDto : holdingsBySymbol) {
                    holdingDtos.add(mapToTransactionHoldingDto(holdingDto));
                }
            }
        }
        return holdingDtos;
    }

    private TransactionHoldingDto mapToTransactionHoldingDto(Holding holding) {
        BigDecimal amount = holding.getAmount() != null ? holding.getAmount() : BigDecimal.ZERO;
        Map<String, Double> prices = pricingFacade.getPrices(holding.getSymbol());
        BigDecimal priceInBtc = BigDecimal.valueOf(prices.getOrDefault(BTC, 0.0));
        BigDecimal priceInUsdt = BigDecimal.valueOf(prices.getOrDefault(USDT, 0.0));

        return TransactionHoldingDto.builder()
                .symbol(holding.getSymbol())
                .amount(amount)
                .priceInBtc(priceInBtc)
                .amountInBtc(amount.multiply(priceInBtc).setScale(8, RoundingMode.HALF_UP))
                .priceInUsdt(priceInUsdt)
                .amountInUsdt(amount.multiply(priceInUsdt).setScale(8, RoundingMode.HALF_UP))
                .stableTotalCost(holding.getStableTotalCost())
                .totalRealizedProfitUsdt(holding.getTotalRealizedProfitUsdt())
                .currentPositionInUsdt(amount.multiply(priceInUsdt).setScale(2, RoundingMode.HALF_UP))
                .percentage(holding.getPercent())
                .build();
    }

    private TransactionHoldingDto mapToTransactionHoldingDto(HoldingDto holdingDto) {
        BigDecimal amount = holdingDto.getAmount() != null ? holdingDto.getAmount() : BigDecimal.ZERO;
        Map<String, Double> prices = pricingFacade.getPrices(holdingDto.getSymbol());
        BigDecimal priceInBtc = BigDecimal.valueOf(prices.getOrDefault(BTC, 0.0));
        BigDecimal priceInUsdt = BigDecimal.valueOf(prices.getOrDefault(USDT, 0.0));

        return TransactionHoldingDto.builder()
                .symbol(holdingDto.getSymbol())
                .amount(amount)
                .priceInBtc(priceInBtc)
                .amountInBtc(amount.multiply(priceInBtc).setScale(8, RoundingMode.HALF_UP))
                .priceInUsdt(priceInUsdt)
                .amountInUsdt(amount.multiply(priceInUsdt).setScale(8, RoundingMode.HALF_UP))
                .stableTotalCost(holdingDto.getStableTotalCost())
                .totalRealizedProfitUsdt(holdingDto.getTotalRealizedProfitUsdt())
                .currentPositionInUsdt(amount.multiply(priceInUsdt).setScale(2, RoundingMode.HALF_UP))
                .percentage(holdingDto.getPercentage())
                .build();
    }

    public Transaction save(TransactionDto transactionDto) {
        Transaction transaction = TransactionConverter.Mapper.createTo(transactionDto);
        
        if (transaction.getPortfolio() == null && transactionDto.getPortfolioName() != null) {
            transaction.setPortfolio(portfolioService.findOrSave(transactionDto.getPortfolioName()));
        }

        if (transaction.getPrice() == null ||
                Strings.isNullOrEmpty(transaction.getPaidWith()) ||
                Strings.isNullOrEmpty(transaction.getPair())) {
            BigDecimal priceInUsdt = pricingFacade.getPriceInUsdt(transaction.getSymbol(), transaction.getDateUtc());
            transaction.setPrice(priceInUsdt);
            transaction.setPair(transaction.getSymbol() + USDT);
            transaction.setPaidWith(USDT);
            transaction.setPaidAmount(transaction.getExecuted().multiply(priceInUsdt));
        }
        return transactionProcessor.process(transaction);
    }

    public void deleteTransactions() {
        transactionService.deleteTransactions();
    }

    public Page<Transaction> filterTransactions(String symbol, String portfolioName, String side, String paidWith, String paidAmountOperator, BigDecimal paidAmount, LocalDate startDate, LocalDate endDate, UUID id, Pageable pageable) {
        return transactionService.filterTransactions(symbol, portfolioName, side, paidWith, paidAmountOperator, paidAmount, startDate, endDate, id, pageable);
    }
}
