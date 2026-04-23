# InvestTracker Development Roadmap

This roadmap outlines the strategic steps to evolve InvestTracker into a world-class portfolio accounting platform.

## Phase 1: Consolidation & Debt Reduction (Completed)
**Goal**: Stabilize the core and eliminate redundant logic.

1.  **Refactor `TransactionFacade`**: (DONE)
    - Migration of `buildPortfolio` and `getAmount` to use the `HoldingService` and `TransactionProcessor` instead of re-calculating everything on the fly.
    - Standardize the `TransactionHoldingDto` to align with the `Holding` entity.
2.  **Audit `PricingFacade`**: (DONE)
    - Improve error handling for failed API calls.
    - Implement caching for historical prices to reduce API consumption and improve performance (In-memory caching with Caffeine and optimized DB scans).
3.  **Cleanup Legacy Code**: (DONE)
    - Remove `ProcessFileV1` as `ProcessFileV2` covers all use cases.
    - Eliminate unused DTOs and old utility methods (Removed `CalculateAmountSpent`).
4.  **Test & Verification (Mandatory)**: (DONE)
    - Fix regression in `BinanceTransactionAdapter` for quoted CSV values.
    - Align `HoldingServiceSpec` and `ProcessFileV2Spec` with the new architecture.
    - Ensure BDD scenarios in `docs/scenarios.md` are covered by automated tests.

## Phase 2: Feature Expansion (2 - 4 Months)
**Goal**: Add value for advanced crypto investors.

1.  **Multi-Currency Base Support**:
    - Allow users to view their portfolio in EUR, GBP, or other fiat currencies, not just USDT.
2.  **Advanced Portfolio Analytics**:
    - **Time-Weighted Return (TWR)** and **Money-Weighted Return (MWR)**.
    - **Drawdown Analysis**: Track the worst peak-to-trough declines.
    - **Sector Allocation**: Categorize coins (DeFi, L1, L2, Gaming) for better diversification insights.
3.  **Tax Reporting Module**:
    - Export transactions in formats compatible with major tax software (Koinly, CoinTracker).
    - Basic Capital Gains report generation.
4.  **Test & Verification (Mandatory)**:
    - Add unit and integration tests for new currency conversion logic.
    - Verify TWR/MWR calculations against manual accounting benchmarks.

## Phase 3: Infrastructure & Ecosystem (5+ Months)
**Goal**: Scalability and professional-grade operation.

1.  **API Versioning**:
    - Implement `/v1/`, `/v2/` pathing for breaking changes.
2.  **Authentication & Multi-Tenancy**:
    - Robust JWT-based multi-tenancy to ensure user data isolation.
    - Social login integration.
3.  **Real-time Updates**:
    - WebSocket integration for live price updates in the UI (if a frontend exists).
4.  **Exchange Syncing (Read-only API Keys)**:
    - Automate transaction ingestion by connecting directly to Binance/MEXC/Coinbase APIs via read-only keys.
5.  **Test & Verification (Mandatory)**:
    - Load testing for high-volume transaction syncing.
    - Security audit and penetration testing for multi-tenancy isolation.

---

## Expert Recommendations (Tech Lead View)

- **Shift to Event-Driven Holding Updates**: Instead of processing transactions synchronously during upload, consider a background job (using Spring Events or a Queue) to update holdings. This improves API responsiveness for large file uploads.
- **Database Indexing**: As the `transactions` table grows, ensure composite indexes on `(portfolio_id, symbol, date_utc)` are optimized for filtering.
- **Circuit Breakers**: Implement Resilience4j around the `CryptoCompareProxy` to prevent the app from hanging if the external API is down.
