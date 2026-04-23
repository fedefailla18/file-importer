# InvestTracker Engineering Standard (IES)

This document serves as the "source of truth" for development within the InvestTracker project. It defines the architectural principles, coding standards, and development workflows to be followed by all contributors.

## 1. Core Architectural Principles

> **Note**: For the project roadmap and future steps, see [Roadmap](roadmap.md).

### 1.1 Layered Responsibility
We follow a strict layered architecture to ensure separation of concerns:
- **Controller**: Endpoint definition, request validation, and OpenAPI documentation.
- **Facade**: Orchestration of multiple services. Should NOT contain core business logic.
- **Service**: Core business logic. Domain-specific rules (e.g., accounting, parsing).
- **Repository**: Data access.
- **Entity**: Database mapping.

### 1.2 The "Single Source of Truth" Rule
All accounting logic (Cost Basis, Profit/Loss, Holding updates) **MUST** reside in `TransactionProcessor`.
- Avoid calculating holdings or profits on-the-fly in Controllers or Facades.
- Always use the `Holding` entity as the state for current positions.
- **Investor Focus**: Ensure logic supports "Realized Loss" scenarios and cost basis preservation during volatile market cycles.

### 1.3 DRY (Don't Repeat Yourself)
If logic for calculating "Amount in USDT" or "Price conversion" exists in `OperationUtils` or `PricingFacade`, use it. Do not reimplement it in multiple services.

---

## 2. Coding Standards

### 2.1 Java 11 + Spring Boot
- Use modern Java features where applicable.
- Leverage Lombok to reduce boilerplate (prefer `@RequiredArgsConstructor` for dependency injection).

### 2.2 Clean Code & SOLID
- **Single Responsibility**: Each class should have one reason to change. (e.g., `ProcessFileV2` handles file flow, `TransactionProcessor` handles accounting logic).
- **Open/Closed**: Use interfaces and factories (like `ProcessFileFactory`) to allow extension without modification.
- **Explicit Naming**: Use clear, domain-driven names (`stableTotalCost` instead of `cost`).

### 2.3 Accounting Precision
- Always use `BigDecimal` for financial calculations.
- Use a scale of at least 8 decimals for crypto amounts and 10 for intermediate calculations.
- Standardize rounding to `HALF_UP`.

---

## 3. Testing Strategy

### 3.1 Spock + Groovy
- All new features **MUST** include Spock specifications.
- Use `given/when/then` blocks to make tests readable as documentation.
- **Unit Tests**: Test logic in isolation using `Mock()`.
- **Integration Tests**: Use `BaseIntegrationSpec` (TestContainers) for repository and end-to-end flow testing.

### 3.2 BDD (Behavior Driven Development)
- Complex scenarios should be documented in `docs/scenarios.md` first.
- The code implementation should directly satisfy these scenarios.

---

## 4. Documentation

### 4.1 Documentation First
- Every new feature or significant change must be documented in the `/docs` folder.
- Keep `architecture-and-flow.md` updated with Mermaid diagrams for new flows.

### 4.2 API Documentation
- Every Controller method must have `@Operation` and `@ApiResponse` annotations.
- Document all parameters and DTOs using `@Schema`.

---

## 5. Definition of Done (DoD)
A task is considered done when:
1. Code follows IES principles.
2. Unit and/or Integration tests pass.
3. Test coverage meets the target (80% line).
4. Documentation is updated.
5. Swagger UI reflects the changes accurately.
