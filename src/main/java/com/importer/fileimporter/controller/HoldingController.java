package com.importer.fileimporter.controller;

import com.importer.fileimporter.dto.HoldingDto;
import com.importer.fileimporter.facade.PortfolioDistributionFacade;
import com.importer.fileimporter.payload.request.AddHoldingRequest;
import com.importer.fileimporter.service.HoldingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping(value = "/holding")
@RequiredArgsConstructor
public class HoldingController {

    private final PortfolioDistributionFacade portfolioDistributionFacade;
    private final HoldingService holdingService;

    @GetMapping
    public List<HoldingDto> getHoldings(String symbol) {
        return holdingService.getBySymbol(symbol);
    }

    @PostMapping("/add")
    public HoldingDto addSymbolAmountToPortfolio(@RequestBody AddHoldingRequest addHoldingRequest) {
        return portfolioDistributionFacade.addPortfolioHolding(addHoldingRequest);
    }

    @PostMapping("/addMultiple")
    public List<HoldingDto> addMultipleHoldings(@RequestBody @Valid List<AddHoldingRequest> requests) {
        return portfolioDistributionFacade.addPortfolioHolding(requests);
    }
}
