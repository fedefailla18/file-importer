# API Documentation Guide

This guide provides instructions for documenting APIs in the File Importer project using OpenAPI 3.0 annotations.

## Overview

We use [SpringDoc OpenAPI](https://springdoc.org/) to generate API documentation based on annotations in the code. This documentation is automatically exposed through Swagger UI at `http://localhost:8080/swagger-ui.html` when the application is running.

## Basic Annotations

### Class-Level Annotations

Add `@Tag` to controller classes to group related endpoints:

```java
@RestController
@RequestMapping("/transaction")
@Tag(name = "Transaction", description = "Transaction management APIs")
public class TransactionController {
    // ...
}
```

### Method-Level Annotations

For each API endpoint, add the following annotations:

1. `@Operation` - Describes the operation
2. `@ApiResponses` - Documents possible response codes
3. `@Parameter` - Documents parameters

Example:

```java
@Operation(summary = "Filter transactions", description = "Filter transactions by various criteria with pagination")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Successfully retrieved transactions",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
})
@GetMapping(value = "/filter")
public Page<Transaction> getTransactionsRangeDate(
        @Parameter(description = "Symbol to filter by") @RequestParam(required = false) String symbol
        // Other parameters...
) {
    // Method implementation
}
```

## Common Annotations

| Annotation | Purpose | Example |
|------------|---------|---------|
| `@Tag` | Group related operations | `@Tag(name = "Transaction", description = "Transaction management APIs")` |
| `@Operation` | Describe an endpoint | `@Operation(summary = "Add a new transaction", description = "Manually add a new transaction")` |
| `@ApiResponse` | Document a response code | `@ApiResponse(responseCode = "201", description = "Transaction created")` |
| `@Parameter` | Document a parameter | `@Parameter(description = "Portfolio name", required = true)` |
| `@Schema` | Document a model | `@Schema(implementation = Transaction.class)` |

## Best Practices

1. **Be Consistent**: Use consistent naming and descriptions across all APIs
2. **Be Descriptive**: Provide clear, concise descriptions for operations, parameters, and responses
3. **Document All Responses**: Include all possible response codes, not just success cases
4. **Use Schemas**: Reference model classes with `@Schema` to document response structures
5. **Group Related APIs**: Use meaningful tags to group related endpoints

## Example Controller

See `TransactionController.java` for a complete example of a well-documented controller.

## Additional Resources

- [SpringDoc Documentation](https://springdoc.org/)
- [OpenAPI 3.0 Specification](https://spec.openapis.org/oas/v3.0.3)
- [Swagger UI](https://swagger.io/tools/swagger-ui/)
