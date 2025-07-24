package com.importer.fileimporter.controller

import com.importer.fileimporter.BaseIntegrationSpec
import com.importer.fileimporter.dto.TransactionDto
import com.importer.fileimporter.entity.Portfolio
import com.importer.fileimporter.entity.Transaction
import io.restassured.http.ContentType
import org.springframework.http.HttpStatus

import java.time.LocalDateTime

import static io.restassured.RestAssured.given

class TransactionControllerIntegrationSpec extends BaseIntegrationSpec {


    Portfolio portfolio
    Portfolio newPortfolio

    def setup() {

        portfolio = Portfolio.builder()
                .name("TestPortfolio")
                .creationDate(LocalDateTime.now())
                .build()
        portfolioRepository.save(portfolio)

        newPortfolio = Portfolio.builder()
                .name("NewPortfolio")
                .creationDate(LocalDateTime.now())
                .build()
        portfolioRepository.save(newPortfolio)

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

    def "test creating a transaction with portfolio"() {
        given: "A transaction DTO with portfolio name"
        def transactionDto = TransactionDto.builder()
            .symbol("ADA")
            .side("BUY")
            .executed(new BigDecimal("100"))
            .dateUtc(LocalDateTime.now())
            .build()

        when: "The transaction is saved to the portfolio"
        def response = given()
            .contentType(ContentType.JSON)
            .body(objectMapper.writeValueAsString(transactionDto))
            .when()
            .post("/transaction/${newPortfolio.name}")
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .extract()
            .body()
            .as(Transaction)

        then: "The transaction is saved with the correct portfolio"
        response != null
        response.portfolio != null
        response.portfolio.name == newPortfolio.name
        response.symbol == "ADA"

        and: "The transaction can be retrieved from the repository"
        def retrievedTransaction = transactionRepository.findById(response.id).orElse(null)
        retrievedTransaction != null
        retrievedTransaction.portfolio != null
        retrievedTransaction.portfolio.name == newPortfolio.name
    }
}
