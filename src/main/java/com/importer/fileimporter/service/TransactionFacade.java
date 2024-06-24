package com.importer.fileimporter.service;

import com.importer.fileimporter.dto.TransactionHoldingDto;
import com.importer.fileimporter.entity.Transaction;
import com.importer.fileimporter.facade.PricingFacade;
import com.importer.fileimporter.service.usecase.CalculateAmountSpent;
import com.importer.fileimporter.utils.OperationUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.transaction.NotSupportedException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.importer.fileimporter.service.GetSymbolHistoricPriceService.BTC;
import static com.importer.fileimporter.service.GetSymbolHistoricPriceService.USDT;

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
    private final PricingFacade pricingFacade;

    public List<TransactionHoldingDto> buildPortfolio(List<String> symbols) {
        List<TransactionHoldingDto> holdingDtos = new ArrayList<>();
        for (String symbol : symbols) {
            List<Transaction> transactions = transactionService.getAllBySymbol(symbol, Pageable.unpaged())
                    .getContent().stream()
                    .sorted(Comparator.comparing(t -> t.getTransactionId().getDateUtc()))
                    .collect(Collectors.toList());
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
            List<BigDecimal> ponderancePrice = new ArrayList<>();
            List<BigDecimal> ponderancePriceInBtc = new ArrayList<>();
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
//                            priceInUsdt = getSymbolHistoricPriceService.getPricesAtDate(symbol, USDT, dateUtc);
                            priceInUsdt = pricingFacade.getPrice(symbol, USDT, dateUtc);
                            payedAmountInUsdt = priceInUsdt.multiply(executed);
                            payedAmountInBtc = payedAmount;
                            priceInBtc = price;
                        } else if (USDT.equals(payedWith)) {
                            //priceInBtc = getSymbolHistoricPriceService.getPricesAtDate(symbol, BTC, dateUtc);
                            priceInBtc = pricingFacade.getPrice(symbol, BTC, dateUtc);
                            payedAmountInBtc = priceInBtc.multiply(executed);
                            payedAmountInUsdt = payedAmount;
                            priceInUsdt = price;
                        }

                        boolean isBuy = OperationUtils.isBuy(tr.getTransactionId().getSide());
                        BigDecimal proportion = executed.divide(totalAmount.get(), 13, RoundingMode.UP);
                        if (isBuy) {
                            BigDecimal buyPrice = holdingDto.getBuyPrice().add(proportion.multiply(priceInUsdt));
                            holdingDto.setBuyPrice(buyPrice);
                            BigDecimal buyPriceInBtc = holdingDto.getBuyPriceInBtc().add(proportion.multiply(priceInBtc));
                            holdingDto.setBuyPriceInBtc(buyPriceInBtc);
                            holdingDto.setPayedInUsdt(holdingDto.getPayedInUsdt().add(payedAmountInUsdt));
                            holdingDto.setPayedInBtc(holdingDto.getPayedInBtc().add(payedAmountInBtc));
                        } else {
                            BigDecimal sellPrice = holdingDto.getSellPrice().add(proportion.multiply(priceInUsdt));
                            holdingDto.setSellPrice(sellPrice);
                            BigDecimal sellPriceInBtc = holdingDto.getSellPriceInBtc().add(proportion.multiply(priceInBtc));
                            holdingDto.setSellPriceInBtc(sellPriceInBtc);
                            holdingDto.setPayedInUsdt(holdingDto.getPayedInUsdt().subtract(payedAmountInUsdt));
                            holdingDto.setPayedInBtc(holdingDto.getPayedInBtc().subtract(payedAmountInBtc));
                        }
                    });

//            Map<String, Double> price = getSymbolHistoricPriceService.getPrice(symbol);
            Map<String, Double> price = pricingFacade.getPrices(symbol);
            holdingDto.setPriceInBtc(BigDecimal.valueOf(price.get(BTC)));
            holdingDto.setAmountInBtc(totalAmount.get().multiply(BigDecimal.valueOf(price.get(BTC))));

            holdingDto.setPriceInUsdt(BigDecimal.valueOf(price.get(USDT)));
            holdingDto.setAmountInUsdt(totalAmount.get().multiply(BigDecimal.valueOf(price.get(USDT))));
            holdingDtos.add(holdingDto);
        }
        return holdingDtos;
    }

    @SneakyThrows
    public List<TransactionHoldingDto> getAmount(List<String> symbols) {
        List<TransactionHoldingDto> holdingDtos = new ArrayList<>();
        if (CollectionUtils.isEmpty(symbols)) {
            // need to fetch all transactions and group them
            transactionService.getAll();
            throw new NotSupportedException();
        }
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
