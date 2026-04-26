package com.importer.fileimporter

import com.importer.fileimporter.facade.PortfolioDistributionFacade
import com.importer.fileimporter.facade.PricingFacade
import com.importer.fileimporter.repository.PortfolioRepository
import com.importer.fileimporter.repository.PriceHistoryRepository
import com.importer.fileimporter.repository.TransactionRepository
import com.importer.fileimporter.service.CryptoCompareProxy
import com.importer.fileimporter.service.FileImporterService
import com.importer.fileimporter.service.HoldingService
import com.importer.fileimporter.service.ProcessFileFactory
import com.importer.fileimporter.service.TransactionFacade
import com.importer.fileimporter.service.TransactionService
import org.junit.ClassRule
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
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

    @Autowired
    TransactionFacade transactionFacade

    @Autowired
    ProcessFileFactory processFileFactory

    @Autowired
    PortfolioDistributionFacade portfolioDistributionFacade

    @MockBean
    CryptoCompareProxy cryptoCompareProxy

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
    @Shared
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13.1")
            .withDatabaseName("file_importer_schema")
            .withUsername("root")
            .withPassword("password")
            .withExposedPorts(5432)

    static  {
        postgres.start()
        // Set system properties for Spring Boot to use the dynamic ports from TestContainers
        System.setProperty("DB_URL", postgres.getJdbcUrl())
        System.setProperty("DB_USERNAME", postgres.getUsername())
        System.setProperty("DB_PASSWORD", postgres.getPassword())
        
        System.out.println("[DEBUG_LOG] TestContainers JDBC URL: " + postgres.getJdbcUrl())

        // Ensure Hibernate and Liquibase use the same database connection properties
        System.setProperty("spring.datasource.url", postgres.getJdbcUrl())
        System.setProperty("spring.datasource.username", postgres.getUsername())
        System.setProperty("spring.datasource.password", postgres.getPassword())
        System.setProperty("spring.liquibase.url", postgres.getJdbcUrl())
        System.setProperty("spring.liquibase.user", postgres.getUsername())
        System.setProperty("spring.liquibase.password", postgres.getPassword())
    }
}
