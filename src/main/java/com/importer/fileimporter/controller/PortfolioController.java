package com.importer.fileimporter.controller;

import com.importer.fileimporter.dto.HoldingDto;
import com.importer.fileimporter.dto.PortfolioDistribution;
import com.importer.fileimporter.facade.PortfolioDistributionFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/portfolio")
public class PortfolioController {

    private final PortfolioDistributionFacade portfolioDistributionFacade;

    @GetMapping()
    @Operation(summary = "Get portfolio distribution",
            description = "Get portfolio distribution by portfolio name, calculate portfolio holdings")
    public PortfolioDistribution getPortfolio(@RequestParam String name) {
        return portfolioDistributionFacade.getPortfolioByName(name);
    }

    @GetMapping("/names")
    public List<String> getAllPortfolio() {
        return portfolioDistributionFacade.getAllPortfolioNames();
    }

    @PostMapping("/distribution")
    public PortfolioDistribution calculatePortfolioInSymbol(@RequestParam String portfolioName) {
        return portfolioDistributionFacade.calculatePortfolioInBtcAndUsdt(portfolioName);
    }

    @GetMapping("/{portfolioName}/{symbol}")
    public HoldingDto getHolding(@PathVariable String portfolioName,
                                 @PathVariable String symbol) {
        return portfolioDistributionFacade.getHolding(portfolioName, symbol);
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadExcel() {
        return portfolioDistributionFacade.downloadExcel();
    }

}
