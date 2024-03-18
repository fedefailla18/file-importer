package com.importer.fileimporter.service;

import com.importer.fileimporter.entity.Symbol;
import com.importer.fileimporter.repository.SymbolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class SymbolService {

    private final SymbolRepository symbolRepository;

    public Symbol findOrSaveSymbol(String symbol, String name) {
        Optional<Symbol> bySymbol = symbolRepository.findBySymbol(symbol);
        return bySymbol
                .orElseGet(() -> saveBasicEntity(symbol, name));
    }

    public Symbol findSymbol(String symbol) {
        return symbolRepository.findBySymbol(symbol)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Symbol not created."));
    }

    private Symbol saveBasicEntity(String symbol, String name) {
        return symbolRepository.save(Symbol.builder()
                .symbol(symbol)
                .name(name)
                .build());
    }
}
