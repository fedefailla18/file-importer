# Test Coverage Guide

This guide provides information about test coverage in the File Importer project using JaCoCo.

## Overview

We use [JaCoCo](https://www.jacoco.org/jacoco/) to measure and report test coverage in the project. JaCoCo analyzes which lines of code are executed during tests and generates reports showing coverage metrics.

## Running Coverage Reports

### Unit Test Coverage

To generate a coverage report for unit tests:

```bash
./gradlew test jacocoTestReport
```

The report will be available at: `build/reports/jacoco/test/html/index.html`

### Integration Test Coverage

To generate a coverage report for integration tests:

```bash
./gradlew integrationTest jacocoIntegrationTestReport
```

The report will be available at: `build/reports/jacoco/integrationTest/index.html`

### Combined Coverage

To generate a combined coverage report for both unit and integration tests:

```bash
./gradlew jacocoAllTestReport
```

The report will be available at: `build/reports/jacoco/allTests/index.html`

## Understanding Coverage Reports

JaCoCo reports include several metrics:

1. **Instructions**: The smallest unit of execution measured by JaCoCo. This metric counts the number of Java bytecode instructions that were executed.

2. **Branches**: Measures the percentage of executed branches in control structures like `if` statements.

3. **Cyclomatic Complexity**: Measures the complexity of code by counting the number of different paths through a method.

4. **Lines**: The percentage of lines that have been executed at least once.

5. **Methods**: The percentage of methods that have been called at least once.

6. **Classes**: The percentage of classes that have been loaded at least once.

## Coverage Goals

As a general guideline, we aim for the following coverage levels:

- **Line Coverage**: At least 80%
- **Branch Coverage**: At least 70%

## Improving Test Coverage

To improve test coverage:

1. **Focus on Business Logic**: Prioritize testing core business logic and service classes.

2. **Test Edge Cases**: Ensure your tests cover edge cases and error conditions.

3. **Use Parameterized Tests**: For methods that should behave similarly with different inputs.

4. **Mock External Dependencies**: Use mocking frameworks to isolate the code being tested.

5. **Review Uncovered Code**: Regularly review the JaCoCo reports to identify uncovered code.

## Excluding Code from Coverage

Some code may not need to be tested or may be difficult to test. You can exclude classes or methods from coverage analysis by modifying the JaCoCo configuration in `build.gradle`.

Example:

```groovy
jacocoTestReport {
    afterEvaluate {
        classDirectories.setFrom(files(classDirectories.files.collect {
            fileTree(dir: it, exclude: [
                "com/importer/fileimporter/config/**",
                "com/importer/fileimporter/dto/**"
            ])
        }))
    }
}
```

## Additional Resources

- [JaCoCo Documentation](https://www.jacoco.org/jacoco/trunk/doc/)
- [Gradle JaCoCo Plugin](https://docs.gradle.org/current/userguide/jacoco_plugin.html)