# File-importer
File-importer is a useful app written in Java 11 and SpringBoot
to allow the users upload a Binance transactions file and get accurate information.

## Useful resources

To get prices in real time we use:

        https://www.cryptocompare.com/cryptopian/api-keys

## API Documentation

The API is documented using OpenAPI 3.0 (Swagger). After starting the application, you can access the documentation at:

        http://localhost:8080/swagger-ui.html

This provides an interactive interface to explore and test all available endpoints.

For information on how to document new APIs, see the [API Documentation Guide](docs/api-documentation-guide.md).

## Running tests

### Unit tests

To run unit tests:

        ./gradlew test

### Integration tests

To run integration tests:

        ./gradlew integrationTest

### Test coverage

JaCoCo is configured to generate test coverage reports. After running tests, you can find the reports at:

- Unit tests coverage: `build/reports/jacoco/test/html/index.html`
- Integration tests coverage: `build/reports/jacoco/integrationTest/index.html`
- Combined coverage: `build/reports/jacoco/allTests/index.html`

To generate all test coverage reports:

        ./gradlew jacocoAllTestReport

For more information on test coverage and how to improve it, see the [Test Coverage Guide](docs/test-coverage-guide.md).
