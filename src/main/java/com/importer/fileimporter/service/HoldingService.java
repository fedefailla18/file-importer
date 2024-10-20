package com.importer.fileimporter.service;

import com.importer.fileimporter.converter.HoldingConverter;
import com.importer.fileimporter.dto.HoldingDto;
import com.importer.fileimporter.entity.Holding;
import com.importer.fileimporter.entity.Portfolio;
import com.importer.fileimporter.entity.Symbol;
import com.importer.fileimporter.payload.request.AddHoldingRequest;
import com.importer.fileimporter.repository.HoldingRepository;
import com.importer.fileimporter.utils.OperationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class HoldingService {

    private final HoldingRepository holdingRepository;

    public Holding saveSymbolHolding(Symbol symbol, Portfolio portfolio, BigDecimal amount) {
        return getByPortfolioAndSymbol(portfolio, symbol.getSymbol())
                .map(existingHolding -> saveBasicEntity(existingHolding, symbol.getSymbol(), portfolio, amount))
                .orElseGet(() -> saveBasicEntity(symbol.getSymbol(), portfolio, amount));
    }

    public Optional<Holding> getByPortfolioAndSymbol(Portfolio portfolio, String symbol) {
        return getBySymbolAndPortfolioName(portfolio, symbol);
    }

    public Holding getOrCreateByPortfolioAndSymbol(Portfolio portfolio, String symbol) {
        Optional<Holding> bySymbolAndPortfolioName = getBySymbolAndPortfolioName(portfolio, symbol);
        log.info("getting holding for: {}", bySymbolAndPortfolioName
                .map(e -> e.getSymbol() + " - amount: " + e.getAmount())
                .orElse(symbol + ". New Holding!"));
        return bySymbolAndPortfolioName
                .orElse(Holding.builder()
                                .portfolio(portfolio)
                                .symbol(symbol)
                                .amount(BigDecimal.ZERO)
                                .amountInUsdt(BigDecimal.ZERO)
                                .totalAmountSold(BigDecimal.ZERO)
                                .totalAmountBought(BigDecimal.ZERO)
                                .build());
    }

    public void updatePaidWithHolding(boolean isBuy, String paidWithSymbol, BigDecimal paidAmount, Portfolio portfolio, BigDecimal executed, BigDecimal paidInStable) {
        Holding holding = getOrCreateByPortfolioAndSymbol(portfolio, paidWithSymbol);
        BigDecimal oldAmount = holding.getAmount();
        BigDecimal totalAmountSold = holding.getTotalAmountSold();
//        BigDecimal stableTotalCost = holding.getStableTotalCost();

        BigDecimal updatedAmount = OperationUtils.accumulateExecutedAmount(oldAmount, paidAmount, isBuy);
        BigDecimal updatedTotalAmountSold = OperationUtils.accumulateExecutedAmount(totalAmountSold, paidAmount, isBuy);
//        BigDecimal updatedStableTotalCost = OperationUtils.accumulateExecutedAmount(stableTotalCost, paidInStable, isBuy);


        holding.setAmount(updatedAmount);
        holding.setTotalAmountSold(updatedTotalAmountSold);
//        holding.setStableTotalCost(updatedStableTotalCost);

        log.info("Updating {}. oldAmount = {}, updated amount = {}", paidWithSymbol, oldAmount, updatedAmount);
        save(holding);
    }

    public List<Holding> getByPortfolio(Portfolio portfolio) {
        return holdingRepository.findAllByPortfolio(portfolio);
    }

    public HoldingDto updatePercentageHolding(HoldingDto e, Portfolio portfolio) {
        Holding holding = getByPortfolioAndSymbol(portfolio, e.getSymbol())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Symbol not found"));

        holding.setPercent(e.getPercentage());
        holding.setPriceInUsdt(e.getPriceInUsdt());
        holding.setPriceInBtc(e.getPriceInBtc());
        holding.setAmountInBtc(e.getAmountInBtc());
        holding.setAmountInUsdt(e.getAmountInUsdt());
        holding.setModified(LocalDateTime.now());
        holding.setModifiedBy("Updating percentage-updatePercentageHolding");
        Holding saved = holdingRepository.save(holding);
        return HoldingConverter.Mapper.createFrom(saved);
    }

    private Holding saveBasicEntity(String symbol, Portfolio portfolio, BigDecimal amount) {
        return saveBasicEntity(null, symbol, portfolio, amount);
    }

    private Holding saveBasicEntity(Holding e, String symbol, Portfolio portfolio, BigDecimal amount) {
        LocalDateTime now = LocalDateTime.now();
        boolean holdingIsNull = e == null;
        String createdBy = holdingIsNull ? "Adding holding" : e.getCreatedBy();
        String modifiedBy = holdingIsNull ? "Adding holding" : "Modifying holding";
        UUID id = holdingIsNull ? null : e.getId();
        LocalDateTime created = holdingIsNull ? now : e.getCreated();
        BigDecimal priceInBtc = holdingIsNull ? null : e.getPriceInBtc();
        BigDecimal priceInUsdt = holdingIsNull ? null : e.getPriceInUsdt();
        return holdingRepository.save(Holding.builder()
                .id(id)
                .symbol(symbol)
                .portfolio(portfolio)
                .amount(amount)
                .priceInBtc(priceInBtc)
                .priceInUsdt(priceInUsdt)
                .created(created)
                .createdBy(createdBy)
                .modified(now)
                .modifiedBy(modifiedBy)
                .build());
    }

    public Holding getHolding(Portfolio portfolio, String symbol) {
        return holdingRepository.findByPortfolioAndSymbol(portfolio, symbol)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Holding not found"));
    }

    public Holding save(Holding holding) {
        return holdingRepository.save(holding);
    }

    public List<HoldingDto> getBySymbol(String symbol) {
        return holdingRepository.findAllBySymbol(symbol).stream()
                .map(HoldingConverter.Mapper::createFrom)
                .collect(Collectors.toList());
    }

    public List<Holding> saveAll(Portfolio portfolio, List<AddHoldingRequest> requests) {
        List<Holding> fromRequest = HoldingConverter.Mapper.createFromRequest(requests);
        fromRequest.forEach(h -> h.setPortfolio(portfolio));
        return holdingRepository.saveAll(fromRequest);
    }

    private Optional<Holding> getBySymbolAndPortfolioName(Portfolio portfolio, String symbol) {
        return holdingRepository.findBySymbolAndPortfolioName(symbol, portfolio.getName());
    }
}
