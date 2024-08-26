package com.importer.fileimporter

import com.importer.fileimporter.facade.PricingFacade
import com.importer.fileimporter.repository.PortfolioRepository
import com.importer.fileimporter.repository.PriceHistoryRepository
import com.importer.fileimporter.service.FileImporterService
import com.importer.fileimporter.service.HoldingService
import com.importer.fileimporter.service.TransactionService
import com.importer.fileimporter.service.usecase.CalculateAmountSpent
import org.junit.ClassRule
import org.postgresql.Driver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.datasource.SimpleDriverDataSource
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.PostgreSQLContainer
import spock.lang.Specification

@Transactional
@SpringBootTest(classes = FileImporterApplication, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
@AutoConfigureTestEntityManager
@AutoConfigureMockMvc
abstract class BaseIntegrationSpec extends Specification {

    @Autowired
    FileImporterService fileImporterService

    @Autowired
    TransactionService transactionService

    @Autowired
    PortfolioRepository portfolioRepository

    @Autowired
    PriceHistoryRepository priceHistoryRepository

    @Autowired
    TestEntityManager entityManager

    @Autowired
    CalculateAmountSpent calculateAmountSpent

    @Autowired
    PricingFacade pricingFacade

    @Autowired
    HoldingService holdingService

    // Define a PostgreSQL container
    @ClassRule
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13.1")
            .withDatabaseName("file_importer_db")
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

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource("schema.sql")
        )
        populator.execute(dataSource)
    }
}
