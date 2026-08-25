package com.importer.fileimporter.repository;

import com.importer.fileimporter.entity.Holding;
import com.importer.fileimporter.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HoldingRepository extends JpaRepository<Holding, UUID> {

    @Query("select holding " +
            "from Holding holding " +
            "where UPPER(holding.symbol) = UPPER(:symbol) " +
            "and holding.portfolio.name = :portfolio")
    Optional<Holding> findBySymbolIgnoreCaseAndPortfolioName(@Param("symbol") String symbol,
                                                   @Param("portfolio") String portfolio);

    @Query("select holding from Holding holding where UPPER(holding.symbol) = UPPER(:symbol)")
    List<Holding> findAllBySymbolIgnoreCase(@Param("symbol") String symbol);

    List<Holding> findAllByPortfolio(@Param("portfolio") Portfolio portfolio);
}
