package com.importer.fileimporter.service;

import com.importer.fileimporter.dto.TransactionHoldingDto;
import com.importer.fileimporter.entity.Transaction;
import com.importer.fileimporter.service.usecase.CalculateAmountSpent;
import com.importer.fileimporter.utils.OperationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.importer.fileimporter.service.GetSymbolHistoricPriceService.*;
import static com.importer.fileimporter.service.GetSymbolHistoricPriceService.BTC;

@RequiredArgsConstructor
@Service
@Slf4j
public class TransactionFacade {

    private final TransactionService transactionService;
    private final CalculateAmountSpent calculateAmountSpent;
    private final CoinInformationService coinInformationService;
    private final SymbolService symbolService;
    private final PortfolioService portfolioService;
    private final GetSymbolHistoricPriceService getSymbolHistoricPriceService;

    public List<TransactionHoldingDto> buildPortfolio(List<String> symbols) {
        List<TransactionHoldingDto> holdingDtos = new ArrayList<>();
        for (String symbol : symbols) {
            List<Transaction> transactions = transactionService.getAllBySymbol(symbol, Pageable.unpaged())
                    .getContent();
            AtomicReference<BigDecimal> totalAmount = getTotalAmount(transactions);

            TransactionHoldingDto holdingDto = TransactionHoldingDto.builder()
                    .symbol(symbol)
                    .amount(totalAmount.get())
                    .buyPrice(BigDecimal.ZERO)
                    .buyPriceInBtc(BigDecimal.ZERO)
                    .sellPrice(BigDecimal.ZERO)
                    .sellPriceInBtc(BigDecimal.ZERO)
                    .payedInUsdt(BigDecimal.ZERO)
                    .payedInBtc(BigDecimal.ZERO)
                    .build();
            transactions
                    .forEach(tr -> {
                        BigDecimal executed = tr.getTransactionId().getExecuted();
                        BigDecimal payedAmount = tr.getPayedAmount();
                        BigDecimal payedAmountInUsdt = BigDecimal.ZERO;
                        BigDecimal priceInUsdt = BigDecimal.ZERO;
                        BigDecimal payedAmountInBtc = BigDecimal.ZERO;
                        BigDecimal priceInBtc = BigDecimal.ZERO;
                        String payedWith = tr.getPayedWith();
                        BigDecimal price = tr.getTransactionId().getPrice();
                        LocalDateTime dateUtc = tr.getTransactionId().getDateUtc();
                        if (BTC.equals(payedWith)) {
                            payedAmountInUsdt = getSymbolHistoricPriceService.getPriceInUsdt(symbol, payedAmount, dateUtc);
                            priceInUsdt = getSymbolHistoricPriceService.getPricesAtDate(symbol, USDT, dateUtc);
                        } else if (USDT.equals(payedWith)) {
                            payedAmountInBtc = getSymbolHistoricPriceService.getPriceInBTC(symbol, payedAmount, dateUtc);
                            priceInBtc = getSymbolHistoricPriceService.getPricesAtDate(symbol, BTC, dateUtc);
                        }
                        payedAmountInUsdt = payedAmountInUsdt.equals(BigDecimal.ZERO) ? payedAmount : payedAmountInUsdt;
                        payedAmountInBtc = payedAmountInBtc.equals(BigDecimal.ZERO) ? payedAmount : payedAmountInBtc;
                        priceInUsdt = priceInUsdt.equals(BigDecimal.ZERO) ? price : priceInUsdt;
                        priceInBtc = priceInBtc.equals(BigDecimal.ZERO) ? price : priceInBtc;

                        boolean isBuy = OperationUtils.isBuy(tr.getTransactionId().getSide());
                        BigDecimal ponderance = executed.divide(totalAmount.get(), 7, RoundingMode.DOWN);
                        if (isBuy) {
                            BigDecimal buyPrice = holdingDto.getBuyPrice().add(ponderance.multiply(priceInUsdt));
                            holdingDto.setBuyPrice(buyPrice);
                            BigDecimal buyPriceInBtc = holdingDto.getBuyPriceInBtc().add(ponderance.multiply(priceInBtc));
                            holdingDto.setBuyPriceInBtc(buyPriceInBtc);
                            holdingDto.setPayedInUsdt(holdingDto.getPayedInUsdt().add(payedAmountInUsdt));
                            holdingDto.setPayedInBtc(holdingDto.getPayedInBtc().add(payedAmountInBtc));
                        } else {
                            BigDecimal sellPrice = holdingDto.getSellPrice().add(ponderance.multiply(priceInUsdt));
                            holdingDto.setSellPrice(sellPrice);
                            BigDecimal sellPriceInBtc = holdingDto.getSellPriceInBtc().add(ponderance.multiply(priceInBtc));
                            holdingDto.setSellPriceInBtc(sellPriceInBtc);
                            holdingDto.setPayedInUsdt(holdingDto.getPayedInUsdt().subtract(payedAmountInUsdt));
                            holdingDto.setPayedInBtc(holdingDto.getPayedInBtc().subtract(payedAmountInBtc));
                        }
                    });

            Map<String, Double> price = getSymbolHistoricPriceService.getPrice(symbol);
            holdingDto.setPriceInBtc(BigDecimal.valueOf(price.get(BTC)));
            holdingDto.setAmountInBtc(totalAmount.get().multiply(BigDecimal.valueOf(price.get(BTC))));

            holdingDto.setPriceInUsdt(BigDecimal.valueOf(price.get(USDT)));
            holdingDto.setAmountInUsdt(totalAmount.get().multiply(BigDecimal.valueOf(price.get(USDT))));
            holdingDtos.add(holdingDto);
        }
        return holdingDtos;
    }

    public List<TransactionHoldingDto> getAmount(List<String> symbols) {
        List<TransactionHoldingDto> holdingDtos = new ArrayList<>();
        for (String symbol : symbols) {
            List<Transaction> transactions = transactionService.getAllBySymbol(symbol, Pageable.unpaged())
                    .getContent();
            AtomicReference<BigDecimal> totalAmount = getTotalAmount(transactions);

            TransactionHoldingDto holdingDto = TransactionHoldingDto.builder()
                    .symbol(symbol)
                    .amount(totalAmount.get())
                    .buyPrice(BigDecimal.ZERO)
                    .buyPriceInBtc(BigDecimal.ZERO)
                    .sellPrice(BigDecimal.ZERO)
                    .sellPriceInBtc(BigDecimal.ZERO)
                    .payedInUsdt(BigDecimal.ZERO)
                    .payedInBtc(BigDecimal.ZERO)
                    .build();

            Map<String, Double> price = getSymbolHistoricPriceService.getPrice(symbol);
            holdingDto.setPriceInBtc(BigDecimal.valueOf(price.get(BTC)));
            holdingDto.setAmountInBtc(totalAmount.get().multiply(BigDecimal.valueOf(price.get(BTC))));

            holdingDto.setPriceInUsdt(BigDecimal.valueOf(price.get(USDT)));
            holdingDto.setAmountInUsdt(totalAmount.get().multiply(BigDecimal.valueOf(price.get(USDT))));
            holdingDtos.add(holdingDto);
        }
        return holdingDtos;
    }

    AtomicReference<BigDecimal> getTotalAmount(List<Transaction> transactions) {
        AtomicReference<BigDecimal> totalAmount = new AtomicReference<>(BigDecimal.ZERO);
        transactions
                .forEach(tr -> {
                    // 1 calculate total amount
                    String side = tr.getTransactionId().getSide();
                    BigDecimal executed = tr.getTransactionId().getExecuted();
                    totalAmount.set(OperationUtils.accumulateExecutedAmount(totalAmount.get(), executed, side));
                });
        return totalAmount;
    }


}
