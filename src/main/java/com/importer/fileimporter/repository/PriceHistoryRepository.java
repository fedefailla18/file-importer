package com.importer.fileimporter.repository;

import com.importer.fileimporter.entity.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, UUID> {

    @Query("SELECT ph " +
            "FROM PriceHistory ph " +
            "WHERE ph.symbol = :symbol " +
            "AND ph.symbolpair = :symbolPair " +
            "AND to_char(ph.time, 'YYYY-MM-DD HH24:MI') LIKE CONCAT(:time, '%')")
    List<PriceHistory> findAllBySymbolAndSymbolpairAndTime(@Param("symbol") String symbol,
                                                           @Param("symbolPair") String symbolPair,
                                                           @Param("time") String time);

    @Query(value = "SELECT * " +
            "FROM price_history ph " +
            "WHERE ph.symbol = :symbol " +
            "AND ph.symbolpair = :symbolPair " +
            "AND DATE_TRUNC('hour', ph.time) = :time", nativeQuery = true)
    List<PriceHistory> findAllBySymbolAndSymbolpairAndTime(@Param("symbol") String symbol,
                                                           @Param("symbolPair") String symbolPair,
                                                           @Param("time") LocalDateTime time);

}
