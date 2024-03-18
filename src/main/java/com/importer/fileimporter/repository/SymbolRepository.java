package com.importer.fileimporter.repository;

import com.importer.fileimporter.entity.Symbol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SymbolRepository extends JpaRepository<Symbol, UUID> {

    @Override
    List<Symbol> findAll();

    Optional<Symbol> findBySymbol(String symbol);

}
