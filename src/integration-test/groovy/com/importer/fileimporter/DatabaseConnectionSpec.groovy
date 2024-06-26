package com.importer.fileimporter


import org.postgresql.Driver
import org.springframework.jdbc.datasource.SimpleDriverDataSource

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class DatabaseConnectionSpec extends BaseIntegrationSpec {

    def "test database connection"() {
        given: "a datasource"
        def dataSource = new SimpleDriverDataSource(
                new Driver(),
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
        )

        when: "a connection is obtained"
        def connection = dataSource.getConnection()

        then: "the connection is valid"
        assert connection != null
        assert !connection.isClosed()

        try (Connection connectionTest = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
            System.out.println("Connection successful!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
