package com.importer.fileimporter.repository

import com.importer.fileimporter.BaseIntegrationSpec
import com.importer.fileimporter.utils.PriceHistoryFactory

class PriceHistoryRepositoryIntegrationSpec extends BaseIntegrationSpec {

    def "Save and retrieve PriceHistory"() {
        given:
        def priceHistory = PriceHistoryFactory.createEntity()

        when:
        def savedPriceHistory = entityManager.persistAndFlush(priceHistory)
        def retrievedPriceHistory = priceHistoryRepository.findById(savedPriceHistory.id)

        then:
        retrievedPriceHistory.isPresent()
        retrievedPriceHistory.get().name == "Sample Price History"
        retrievedPriceHistory.get().pair == "Sample Pair"
    }
}
