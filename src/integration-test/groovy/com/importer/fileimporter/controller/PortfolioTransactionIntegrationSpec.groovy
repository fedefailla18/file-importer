package com.importer.fileimporter.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.importer.fileimporter.BaseIntegrationSpec
import com.importer.fileimporter.entity.Portfolio
import com.importer.fileimporter.entity.Transaction
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus

import java.time.LocalDateTime

import static io.restassured.RestAssured.given

class PortfolioTransactionIntegrationSpec extends BaseIntegrationSpec {

    Portfolio portfolio

    def setup() {

        portfolio = Portfolio.builder()
                .name("TestPortfolio")
                .creationDate(LocalDateTime.now())
                .build()
        portfolioRepository.save(portfolio)

        // Create test transactions
        def transaction1 = new Transaction(
            symbol: "ETH",
            side: "BUY",
            executed: new BigDecimal("1.5"),
            paidWith: "USDT",
            paidAmount: new BigDecimal("3000"),
            portfolio: portfolio,
            dateUtc: LocalDateTime.now(),
            pair: "ETHUSDT",
            price: new BigDecimal("2000")
        )

        def transaction2 = new Transaction(
            symbol: "BTC",
            side: "BUY",
            executed: new BigDecimal("0.5"),
            paidWith: "USDT",
            paidAmount: new BigDecimal("20000"),
            portfolio: portfolio,
            dateUtc: LocalDateTime.now(),
            pair: "BTCUSDT",
            price: new BigDecimal("40000")
        )

        transactionRepository.save(transaction1)
        transactionRepository.save(transaction2)
    }

    def "test getInformation endpoint with portfolio"() {
        given: "A portfolio with transactions"
        def portfolioName = portfolio.name

        when: "The getInformation endpoint is called with the portfolio name"
        def response = given()
            .contentType(ContentType.JSON)
            .when()
            .post("/transaction/information/all/${portfolioName}")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(List)

        then: "The response contains the expected data"
        response != null
        !response.isEmpty()
        response.size() == 2
        response.find { it.coinName == "ETH" } != null
        response.find { it.coinName == "BTC" } != null

        and: "The transactions are marked as processed"
        def transactions = transactionRepository.findAll()
        transactions.every { it.processed }
    }
}
