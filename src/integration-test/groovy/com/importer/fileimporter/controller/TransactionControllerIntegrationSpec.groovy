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

    def "setup"() {
        def user = userRepository.findByUsername("Test").get()
        portfolio = portfolioRepository.findByName("Test").get()

        newPortfolio = Portfolio.builder()
                .name("NewPortfolio")
                .creationDate(LocalDateTime.now())
                .user(user)
                .build()
        portfolioRepository.save(newPortfolio)

        def transaction1 = new Transaction(
            symbol: "ETH",
            side: "BUY",
            executed: new BigDecimal("1.5"),
            paidWith: "USDT",
            paidAmount: new BigDecimal("3000"),
            portfolio: newPortfolio,
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
            portfolio: newPortfolio,
            dateUtc: LocalDateTime.now(),
            pair: "BTCUSDT",
            price: new BigDecimal("40000")
        )

        transactionRepository.save(transaction1)
        transactionRepository.save(transaction2)
    }

    def "test getInformation endpoint with portfolio"() {
        given: "A portfolio with transactions"
        def portfolioName = newPortfolio.name

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
        def transactions = transactionRepository.findByPortfolio(newPortfolio)
        transactions.every { it.processed }
    }

    def "test getInformation endpoint with portfolio containing buy and sell transactions"() {
        given: "A portfolio with buy and sell transactions"
        def portfolioName = newPortfolio.name

        // Add a sell transaction for ETH
        def sellTransaction = new Transaction(
            symbol: "ETH",
            side: "SELL",
            executed: new BigDecimal("0.5"),
            paidWith: "USDT",
            paidAmount: new BigDecimal("1100"),
            portfolio: newPortfolio,
            dateUtc: LocalDateTime.now(),
            pair: "ETHUSDT",
            price: new BigDecimal("2200")
        )
        transactionRepository.save(sellTransaction)

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

        and: "The ETH data includes both buy and sell transactions"
        def ethInfo = response.find { it.coinName == "ETH" }
        ethInfo != null
        ethInfo.totalAmountBought == 1.5
        ethInfo.totalAmountSold == 0.5
        ethInfo.amount == 1.0 // 1.5 bought - 0.5 sold

        and: "The BTC data is correct"
        def btcInfo = response.find { it.coinName == "BTC" }
        btcInfo != null
        btcInfo.totalAmountBought == 0.5
        btcInfo.amount == 0.5

        and: "All transactions are marked as processed"
        def transactions = transactionRepository.findByPortfolio(newPortfolio)
        transactions.every { it.processed }
    }

    def "test getInformation endpoint with portfolio containing multiple buy transactions at different prices"() {
        given: "A portfolio with multiple buy transactions at different prices"
        def portfolioName = newPortfolio.name

        // Add another BTC transaction at a different price
        def btcTransaction2 = new Transaction(
            symbol: "BTC",
            side: "BUY",
            executed: new BigDecimal("0.25"),
            paidWith: "USDT",
            paidAmount: new BigDecimal("12500"),
            portfolio: newPortfolio,
            dateUtc: LocalDateTime.now(),
            pair: "BTCUSDT",
            price: new BigDecimal("50000")
        )
        transactionRepository.save(btcTransaction2)

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

        and: "The BTC data includes both transactions"
        def btcInfo = response.find { it.coinName == "BTC" }
        btcInfo != null
        btcInfo.totalAmountBought == 0.75 // 0.5 + 0.25
        btcInfo.amount == 0.75

        and: "All transactions are marked as processed"
        def transactions = transactionRepository.findByPortfolio(newPortfolio)
        transactions.every { it.processed }
    }

    def "test getInformation endpoint with portfolio containing multiple coins and complex transactions"() {
        given: "A portfolio with multiple coins and complex transactions"
        def portfolioName = newPortfolio.name

        // Add a new coin (XRP)
        def xrpTransaction1 = new Transaction(
            symbol: "XRP",
            side: "BUY",
            executed: new BigDecimal("1000"),
            paidWith: "USDT",
            paidAmount: new BigDecimal("500"),
            portfolio: newPortfolio,
            dateUtc: LocalDateTime.now().minusDays(5),
            pair: "XRPUSDT",
            price: new BigDecimal("0.5")
        )
        transactionRepository.save(xrpTransaction1)

        // Add another XRP transaction at a different price
        def xrpTransaction2 = new Transaction(
            symbol: "XRP",
            side: "BUY",
            executed: new BigDecimal("2000"),
            paidWith: "USDT",
            paidAmount: new BigDecimal("1200"),
            portfolio: newPortfolio,
            dateUtc: LocalDateTime.now().minusDays(3),
            pair: "XRPUSDT",
            price: new BigDecimal("0.6")
        )
        transactionRepository.save(xrpTransaction2)

        // Add a sell transaction for XRP
        def xrpTransaction3 = new Transaction(
            symbol: "XRP",
            side: "SELL",
            executed: new BigDecimal("500"),
            paidWith: "USDT",
            paidAmount: new BigDecimal("350"),
            portfolio: newPortfolio,
            dateUtc: LocalDateTime.now().minusDays(1),
            pair: "XRPUSDT",
            price: new BigDecimal("0.7")
        )
        transactionRepository.save(xrpTransaction3)

        // Add another ETH transaction
        def ethTransaction = new Transaction(
            symbol: "ETH",
            side: "BUY",
            executed: new BigDecimal("0.75"),
            paidWith: "USDT",
            paidAmount: new BigDecimal("1500"),
            portfolio: newPortfolio,
            dateUtc: LocalDateTime.now().minusDays(2),
            pair: "ETHUSDT",
            price: new BigDecimal("2000")
        )
        transactionRepository.save(ethTransaction)

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
        response.size() == 3  // ETH, BTC, and XRP

        and: "The XRP data is correct"
        def xrpInfo = response.find { it.coinName == "XRP" }
        xrpInfo != null
        xrpInfo.totalAmountBought == 3000  // 1000 + 2000
        xrpInfo.totalAmountSold == 500
        xrpInfo.amount == 2500  // 3000 - 500

        and: "The ETH data is correct"
        def ethInfo = response.find { it.coinName == "ETH" }
        ethInfo != null
        ethInfo.totalAmountBought == 2.25  // 1.5 from setup + 0.75

        and: "The BTC data is correct"
        def btcInfo = response.find { it.coinName == "BTC" }
        btcInfo != null
        btcInfo.totalAmountBought == 0.5

        and: "All transactions are marked as processed"
        def transactions = transactionRepository.findByPortfolio(newPortfolio)
        transactions.every { it.processed }
    }

    def "test creating a transaction with portfolio"() {
        given: "A transaction DTO with portfolio name"
        def transactionDto = TransactionDto.builder()
            .symbol("ADA")
            .side("BUY")
            .executed(new BigDecimal("100"))
            .dateUtc(LocalDateTime.now())
            .pair("ADAUSDT")
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
