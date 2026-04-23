package com.importer.fileimporter

import com.importer.fileimporter.facade.PricingFacade
import com.importer.fileimporter.repository.PortfolioRepository
import com.importer.fileimporter.repository.PriceHistoryRepository
import com.importer.fileimporter.repository.TransactionRepository
import com.importer.fileimporter.service.FileImporterService
import com.importer.fileimporter.service.HoldingService
import com.importer.fileimporter.service.TransactionService
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
import com.importer.fileimporter.entity.User
import com.importer.fileimporter.repository.UserRepository
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import spock.lang.Shared
import spock.lang.Specification

@Transactional
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
@AutoConfigureTestEntityManager
@AutoConfigureMockMvc
abstract class BaseIntegrationSpec extends Specification {

    @Autowired
    FileImporterService fileImporterService

    @Autowired
    TransactionService transactionService

    @Autowired
    TransactionRepository transactionRepository

    @Autowired
    PortfolioRepository portfolioRepository

    @Autowired
    PriceHistoryRepository priceHistoryRepository

    @Autowired
    UserRepository userRepository

    @Autowired
    TestEntityManager entityManager

    @Autowired
    PricingFacade pricingFacade

    @Autowired
    HoldingService holdingService

    @Shared
    User defaultUser

    def setup() {
        if (defaultUser == null) {
            defaultUser = userRepository.findByUsername("default_user").orElseGet({
                def user = User.builder()
                        .username("default_user")
                        .email("default@example.com")
                        .password("change_me")
                        .build()
                userRepository.save(user)
            })
        }
        def auth = new UsernamePasswordAuthenticationToken(defaultUser, null, [])
        SecurityContextHolder.getContext().setAuthentication(auth)
    }

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    // Define a PostgreSQL container
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
