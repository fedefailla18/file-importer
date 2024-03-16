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
        Optional<Holding> holding = getBySymbolAndPortfolioName(portfolio, symbol.getSymbol());
        holding.ifPresent(e -> saveBasicEntity(e, symbol.getSymbol(), portfolio, amount));
        return holding.orElse(saveBasicEntity(null, symbol.getSymbol(), portfolio, amount));
    }

    public Optional<Holding> getBySymbolAndPortfolioName(Portfolio portfolio, String symbol) {
        return holdingRepository.findBySymbolAndPortfolioName(symbol, portfolio.getName());
    }

    public List<Holding> getByPortfolio(Portfolio portfolio) {
        return holdingRepository.findAllByPortfolio(portfolio);
    }

    public HoldingDto updatePercentageHolding(HoldingDto e, Portfolio portfolio) {
        Holding holding = getBySymbolAndPortfolioName(portfolio, e.getSymbol())
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

    private Holding saveBasicEntity(Holding e, String symbol, Portfolio portfolio, BigDecimal amount) {
        String createdBy = e == null ? "Adding holding" : e.getCreatedBy();
        String modifiedBy = e == null ? "Adding holding" : "Modifying holding";
        UUID id = e == null ? null : e.getId();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime created = e == null ? now : e.getCreated();
        return holdingRepository.save(Holding.builder()
                .id(id)
                .symbol(symbol)
                .portfolio(portfolio)
                .amount(amount)
                .priceInBtc(e.getPriceInBtc())
                .priceInUsdt(e.getPriceInUsdt())
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

}
