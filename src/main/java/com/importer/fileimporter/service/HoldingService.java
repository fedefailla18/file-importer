package com.importer.fileimporter.service;

import com.importer.fileimporter.coverter.HoldingConverter;
import com.importer.fileimporter.dto.HoldingDto;
import com.importer.fileimporter.entity.Holding;
import com.importer.fileimporter.entity.Portfolio;
import com.importer.fileimporter.entity.Symbol;
import com.importer.fileimporter.repository.HoldingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class HoldingService {

    private final HoldingRepository holdingRepository;

    public Holding saveSymbolHolding(Symbol symbol, Portfolio portfolio, BigDecimal amount) {
        return getByPortfolioAndSymbol(portfolio, symbol.getSymbol())
                .map(existingHolding -> saveBasicEntity(existingHolding, symbol.getSymbol(), portfolio, amount))
                .orElseGet(() -> saveBasicEntity(symbol.getSymbol(), portfolio, amount));
    }

    public Optional<Holding> getByPortfolioAndSymbol(Portfolio portfolio, String symbol) {
        return holdingRepository.findBySymbolAndPortfolioName(symbol, portfolio.getName());
    }

    public Holding getHoldingByPortfolioAndSymbol(Portfolio portfolio, String symbol) {
        return holdingRepository.findBySymbolAndPortfolioName(symbol, portfolio.getName())
                .orElse(Holding.builder()
                                .portfolio(portfolio)
                                .symbol(symbol)
                                .amount(BigDecimal.ZERO)
                                .amountInUsdt(BigDecimal.ZERO)
                                .build());
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

    public Holding getHolding(String symbol) {
        return holdingRepository.getBySymbol(symbol)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Symbol not found"));
    }

    public Holding save(Holding holding) {
        return holdingRepository.save(holding);
    }
}
