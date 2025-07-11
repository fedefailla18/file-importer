package com.importer.fileimporter.service;

import com.google.common.base.Strings;
import com.importer.fileimporter.converter.TransactionConverter;
import com.importer.fileimporter.dto.TransactionDto;
import com.importer.fileimporter.dto.TransactionHoldingDto;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.importer.fileimporter.utils.OperationUtils.BTC;
import static com.importer.fileimporter.utils.OperationUtils.USDT;

@RequiredArgsConstructor
@Service
@Slf4j
public class TransactionFacade {

    private final TransactionService transactionService;
    private final PricingFacade pricingFacade;

    public List<TransactionHoldingDto> buildPortfolio(List<String> symbols) {
        List<TransactionHoldingDto> holdingDtos = new ArrayList<>();

        for (String symbol : symbols) {
            List<Transaction> transactions = transactionService.getAllBySymbol(symbol.toUpperCase(), Pageable.unpaged())
                    .getContent().stream()
                    .sorted(Comparator.comparing(Transaction::getDateUtc))
                    .collect(Collectors.toList());
            AtomicReference<BigDecimal> totalAmount = getTotalAmount(transactions);

            TransactionHoldingDto holdingDto = TransactionHoldingDto.emptyTransactionHoldingDto(symbol, totalAmount.get());

            BigDecimal totalBuyAmountInUsdt = BigDecimal.ZERO;
            BigDecimal totalBuyAmountInBtc = BigDecimal.ZERO;
            BigDecimal totalSellAmountInUsdt = BigDecimal.ZERO;
            BigDecimal totalSellAmountInBtc = BigDecimal.ZERO;

            for (Transaction tr : transactions) {
                BigDecimal executed = tr.getExecuted();
                BigDecimal payedAmount = tr.getPaidAmount();
                BigDecimal payedAmountInUsdt = BigDecimal.ZERO;
                BigDecimal priceInUsdt = BigDecimal.ZERO;
                BigDecimal payedAmountInBtc = BigDecimal.ZERO;
                BigDecimal priceInBtc = BigDecimal.ZERO;
                String payedWith = tr.getPaidWith();
                BigDecimal price = tr.getPrice();
                LocalDateTime dateUtc = tr.getDateUtc();

                if (BTC.equals(payedWith)) {
                    priceInUsdt = pricingFacade.getPriceInUsdt(symbol, dateUtc);
                    payedAmountInUsdt = priceInUsdt.multiply(executed).setScale(8, RoundingMode.HALF_UP);
                    payedAmountInBtc = payedAmount;
                    priceInBtc = price;
                } else if (USDT.equals(payedWith)) {
                    priceInBtc = pricingFacade.getPriceInBTC(symbol, dateUtc);
                    payedAmountInBtc = priceInBtc.multiply(executed).setScale(8, RoundingMode.HALF_UP);
                    payedAmountInUsdt = payedAmount;
                    priceInUsdt = price;
                }

                boolean isBuy = OperationUtils.isBuy(tr.getSide());
                // Avoid division by zero
                BigDecimal proportion = totalAmount.get().compareTo(BigDecimal.ZERO) == 0 ? 
                    BigDecimal.ZERO : 
                    executed.divide(totalAmount.get(), 13, RoundingMode.HALF_UP);

                if (isBuy) {
                    totalBuyAmountInUsdt = totalBuyAmountInUsdt.add(payedAmountInUsdt);
                    totalBuyAmountInBtc = totalBuyAmountInBtc.add(payedAmountInBtc);
                    holdingDto.setBuyPrice(holdingDto.getBuyPrice().add(proportion.multiply(priceInUsdt)));
                    holdingDto.setBuyPriceInBtc(holdingDto.getBuyPriceInBtc().add(proportion.multiply(priceInBtc)));
                    holdingDto.setPayedInUsdt(holdingDto.getPayedInUsdt().add(payedAmountInUsdt));
                    holdingDto.setPayedInBtc(holdingDto.getPayedInBtc().add(payedAmountInBtc));
                } else {
                    totalSellAmountInUsdt = totalSellAmountInUsdt.add(payedAmountInUsdt);
                    totalSellAmountInBtc = totalSellAmountInBtc.add(payedAmountInBtc);
                    holdingDto.setSellPrice(holdingDto.getSellPrice().add(proportion.multiply(priceInUsdt)));
                    holdingDto.setSellPriceInBtc(holdingDto.getSellPriceInBtc().add(proportion.multiply(priceInBtc)));
                    holdingDto.setPayedInUsdt(holdingDto.getPayedInUsdt().subtract(payedAmountInUsdt));
                    holdingDto.setPayedInBtc(holdingDto.getPayedInBtc().subtract(payedAmountInBtc));
                }
            }

            // Assuming the use of a map to retrieve current prices
            Map<String, Double> price = pricingFacade.getPrices(symbol);
            BigDecimal priceInBtc = BigDecimal.valueOf(price.get(BTC));
            BigDecimal priceInUsdt = BigDecimal.valueOf(price.get(USDT));

            holdingDto.setPriceInBtc(priceInBtc);
            holdingDto.setAmountInBtc(totalAmount.get().multiply(priceInBtc).setScale(8, RoundingMode.HALF_UP));
            holdingDto.setPriceInUsdt(priceInUsdt);
            holdingDto.setAmountInUsdt(totalAmount.get().multiply(priceInUsdt).setScale(8, RoundingMode.HALF_UP));

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

            Map<String, Double> price = pricingFacade.getPrices(symbol);
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
                    String side = tr.getSide();
                    BigDecimal executed = tr.getExecuted();
                    totalAmount.set(OperationUtils.accumulateExecutedAmount(totalAmount.get(), executed, side));
                });
        return totalAmount;
    }

    public Transaction save(TransactionDto transactionDto) {
        Transaction transaction = TransactionConverter.Mapper.createTo(transactionDto);
        if (transaction.getPrice() == null ||
                Strings.isNullOrEmpty(transaction.getPaidWith()) ||
                Strings.isNullOrEmpty(transaction.getPair())) {
            BigDecimal priceInUsdt = pricingFacade.getPriceInUsdt(transaction.getSymbol(), transaction.getDateUtc());
            transaction.setPrice(priceInUsdt);
            transaction.setPair(transaction.getSymbol() + USDT);
            transaction.setPaidWith(USDT);
            transaction.setPaidAmount(transaction.getExecuted().multiply(priceInUsdt));
        }
        return transactionService.save(transaction);
    }

    public void deleteTransactions() {
        transactionService.deleteTransactions();
    }

    public Page<Transaction> filterTransactions(String symbol, String portfolioName, String side, String paidWith, String paidAmountOperator, BigDecimal paidAmount, LocalDate startDate, LocalDate endDate, UUID id, Pageable pageable) {
        return transactionService.filterTransactions(symbol, portfolioName, side, paidWith, paidAmountOperator, paidAmount, startDate, endDate, id, pageable);
    }
}
