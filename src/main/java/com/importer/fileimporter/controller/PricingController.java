package com.importer.fileimporter.controller;

import com.importer.fileimporter.facade.PricingFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/pricing")
@Slf4j
public class PricingController {

    private final PricingFacade pricingFacade;

    @GetMapping
    public ResponseEntity getPriceAtDate(@RequestParam(required = false) String symbol,
                                       @RequestParam(required = false) String symbolPair,
                                       @RequestParam(required = false)  List<String> symbols,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTime) {
        if (CollectionUtils.isEmpty(symbols)) {
            return ResponseEntity.of(Optional.ofNullable(pricingFacade.getPrice(symbolPair, symbol, dateTime)));
        }
        return ResponseEntity.of(Optional.ofNullable(pricingFacade.getPrices(symbols)));
    }

    @GetMapping("/{symbol}")
    public Map<String, Double> getPrices(@PathVariable String symbol) {
        return pricingFacade.getPrices(symbol);
    }

}