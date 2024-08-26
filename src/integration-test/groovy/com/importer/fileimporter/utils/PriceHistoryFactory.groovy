package com.importer.fileimporter.utils

import com.importer.fileimporter.entity.PriceHistory

import java.time.LocalDateTime

class PriceHistoryFactory {

    static PriceHistory createEntity() {
        return PriceHistory.builder()
                .name("Sample Price History")
                .time(LocalDateTime.now())
                .pair("Sample Pair")
                .symbol("BTC")
                .symbolpair("USD")
                .high(100.0)
                .low(90.0)
                .open(95.0)
                .close(98.0)
                .volumeto(1000.0)
                .volumefrom(500.0)
                .created(LocalDateTime.now())
                .createdBy("Test User")
                .modified(LocalDateTime.now())
                .modifiedBy("Test User")
                .build()
    }
}
