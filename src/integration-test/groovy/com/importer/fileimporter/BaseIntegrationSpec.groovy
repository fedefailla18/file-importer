package com.importer.fileimporter

import com.fasterxml.jackson.databind.ObjectMapper
import com.importer.fileimporter.config.security.jwt.JwtService
import com.importer.fileimporter.controller.TransactionController
import com.importer.fileimporter.entity.User
import com.importer.fileimporter.facade.CoinInformationFacade
import com.importer.fileimporter.facade.PricingFacade
import com.importer.fileimporter.repository.PortfolioRepository
import com.importer.fileimporter.repository.PriceHistoryRepository
import com.importer.fileimporter.repository.TransactionRepository
import com.importer.fileimporter.repository.UserRepository
import com.importer.fileimporter.service.FileImporterService
import com.importer.fileimporter.service.HoldingService
import com.importer.fileimporter.service.TransactionService
import com.importer.fileimporter.service.usecase.CalculateAmountSpent
import io.restassured.RestAssured
import io.restassured.builder.RequestSpecBuilder
import org.junit.ClassRule
import org.postgresql.Driver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.datasource.SimpleDriverDataSource
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.PostgreSQLContainer
import spock.lang.Specification

import javax.persistence.EntityManager

@Transactional
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
@AutoConfigureTestEntityManager
@AutoConfigureMockMvc
abstract class BaseIntegrationSpec extends Specification {

    @Autowired
    protected FileImporterService fileImporterService

    @Autowired
    protected TransactionService transactionService

    @Autowired
    protected TransactionRepository transactionRepository

    @Autowired
    protected PortfolioRepository portfolioRepository

    @Autowired
    protected PriceHistoryRepository priceHistoryRepository

    @Autowired
    protected TestEntityManager entityManager
    @Autowired
    protected EntityManager entityManager1

    @Autowired
    protected CalculateAmountSpent calculateAmountSpent

    @Autowired
    protected PricingFacade pricingFacade

    @Autowired
    protected HoldingService holdingService

    @Autowired
    protected CoinInformationFacade coinInformationFacade

    @Autowired
    protected JwtService jwtService

    @Autowired
    protected MockMvc mockMvc

    @Autowired
    protected ObjectMapper objectMapper

    @Autowired
    protected TransactionController transactionController

    @Autowired
    protected UserRepository userRepository

    @LocalServerPort
    protected int port

    def setup() {
        RestAssured.port = port

        User testUser = userRepository.findByUsername("Test").orElse(null)
        if (testUser) {
            String token = jwtService.generateToken(testUser)
            // Configure RestAssured to include the token in all requests
            RestAssured.requestSpecification = new RequestSpecBuilder()
                    .addHeader("Authorization", "Bearer " + token)
                    .build()
        }
    }

    @ClassRule
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13.1")
            .withDatabaseName("file_importer_schema")
            .withUsername("root")
            .withPassword("password")
            .withExposedPorts(60366)

    static  {

        postgres.setPortBindings(["60366:5432"])
        postgres.start()
        // Execute schema.sql to create the schema
        String jdbcUrl = postgres.getJdbcUrl()
        String username = postgres.getUsername()
        String password = postgres.getPassword()

        // Set system properties for Spring Boot
        System.setProperty("DB_URL", jdbcUrl)
        System.setProperty("DB_USERNAME", username)
        System.setProperty("DB_PASSWORD", password)

        def dataSource = new SimpleDriverDataSource(
                new Driver(),
                jdbcUrl,
                username,
                password
        )

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator()
        populator.addScript(new ClassPathResource("schema.sql"))
        populator.execute(dataSource)
    }
}
