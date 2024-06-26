package com.importer.fileimporter

import com.importer.fileimporter.service.FileImporterService
import org.junit.ClassRule
import org.postgresql.Driver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.datasource.SimpleDriverDataSource
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
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
    MockMvc mockMvc

    @Autowired
    FileImporterService fileImporterService

    // Define a PostgreSQL container
    @ClassRule
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13.1")
            .withDatabaseName("file_importer_db")
            .withUsername("root")
            .withPassword("password")
            .withExposedPorts(60366)

    static  {
        postgres.setPortBindings(["60366:5432"])
//        postgres.withNetworkMode("host")
        postgres.start()
        println("JDBC URL: ${postgres.getJdbcUrl()}")
        println("Username: ${postgres.getUsername()}")
        println("Password: ${postgres.getPassword()}")
        // Execute schema.sql to create the schema
        String jdbcUrl = postgres.getJdbcUrl()
        String username = postgres.getUsername()
        String password = postgres.getPassword()

        // Set system properties for Spring Boot
        System.setProperty("DB_URL", postgres.getJdbcUrl())
        System.setProperty("DB_USERNAME", postgres.getUsername())
        System.setProperty("DB_PASSWORD", postgres.getPassword())

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
