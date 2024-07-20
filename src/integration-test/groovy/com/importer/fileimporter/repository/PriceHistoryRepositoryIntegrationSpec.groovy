package com.importer.fileimporter.repository

import com.importer.fileimporter.BaseIntegrationSpec
import com.importer.fileimporter.entity.PriceHistory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager

import java.time.LocalDateTime

class PriceHistoryRepositoryIntegrationSpec extends BaseIntegrationSpec {

    @Autowired
    TestEntityManager entityManager

    @Autowired
    PriceHistoryRepository priceHistoryRepository

    def "Save and retrieve PriceHistory"() {
        given:
        def priceHistory = PriceHistory.builder()
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

        when:
        def savedPriceHistory = entityManager.persistAndFlush(priceHistory)
        def retrievedPriceHistory = priceHistoryRepository.findById(savedPriceHistory.id)

        then:
        retrievedPriceHistory.isPresent()
        retrievedPriceHistory.get().name == "Sample Price History"
        retrievedPriceHistory.get().pair == "Sample Pair"
    }
}
