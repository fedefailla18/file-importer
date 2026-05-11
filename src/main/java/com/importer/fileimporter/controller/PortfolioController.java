package com.importer.fileimporter.controller;

import com.importer.fileimporter.dto.HoldingDto;
import com.importer.fileimporter.dto.PortfolioDistribution;
import com.importer.fileimporter.entity.ExchangeName;
import com.importer.fileimporter.facade.PortfolioDistributionFacade;
import com.importer.fileimporter.entity.Portfolio;
import com.importer.fileimporter.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/portfolio")
@Slf4j
@Tag(name = "Portfolio", description = "Portfolio management APIs")
public class PortfolioController {

    private final PortfolioDistributionFacade portfolioDistributionFacade;
    private final PortfolioService portfolioService;

    @Operation(summary = "Create a new portfolio", description = "Create a single portfolio for the current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Portfolio successfully created",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Portfolio.class))),
            @ApiResponse(responseCode = "400", description = "Invalid portfolio data", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @PostMapping("/{name}")
    @ResponseStatus(HttpStatus.CREATED)
    public Portfolio createPortfolio(
            @Parameter(description = "Portfolio name", required = true) @PathVariable String name,
            @Parameter(description = "Optional exchange source (BINANCE, MEXC)") @RequestParam(required = false) ExchangeName exchangeName) {
        log.info("REST request to create portfolio: {}", name);
        return portfolioService.findOrSave(name, exchangeName);
    }

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
