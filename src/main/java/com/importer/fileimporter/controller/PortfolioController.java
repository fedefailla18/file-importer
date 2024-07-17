package com.importer.fileimporter.controller;

import com.importer.fileimporter.dto.HoldingDto;
import com.importer.fileimporter.dto.PortfolioDistribution;
import com.importer.fileimporter.facade.PortfolioDistributionFacade;
import com.importer.fileimporter.payload.request.AddHoldingRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/portfolio")
@Slf4j
public class PortfolioController {

    private final PortfolioDistributionFacade portfolioDistributionFacade;

    @PostMapping("/add")
    public HoldingDto addSymbolAmountToPortfolio(@RequestBody AddHoldingRequest addHoldingRequest) {
        log.info("adding holding. " + addHoldingRequest.getSymbol());
        return portfolioDistributionFacade.addPortfolioHolding(addHoldingRequest);
    }

    @GetMapping()
    public PortfolioDistribution getPortfolio(@RequestParam String name) {
        return portfolioDistributionFacade.getPortfolioByName(name);
    }

    @PostMapping("/holding/distribution")
    public PortfolioDistribution calculatePortfolioInSymbol(@RequestParam String name) {
        return portfolioDistributionFacade.calculatePortfolioInBtcAndUsdt(name);
    }

    @PostMapping("/holding/distributions")
    public List<PortfolioDistribution> calculatePortfolioInSymbol() {
        return portfolioDistributionFacade.
                calculatePortfolioInBtcAndUsdt();
    }

    @GetMapping("/holding")
    public HoldingDto getHolding(@RequestParam String symbol) {
        return portfolioDistributionFacade.getHolding(symbol);
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadExcel() {
        return portfolioDistributionFacade.downloadExcel();
    }

}
