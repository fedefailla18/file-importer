# PR #60 Migration Guide: Eager → Lazy Transaction Processing

## What Changed?

### Deprecated (Removed)
- `TransactionProcessor.process()` — replaced by on-demand lazy evaluation
- Automatic transaction processing on upload

### New
- `/transactions/unprocess/{symbol}` — manual recalculation trigger
- `CoinInformationService` lazy evaluation model
- `CalculateAmountSpent.getAmountInUsdt()` — refactored for non-stable coins

### Renamed (Breaking Change)
- `stableTotalCost` → `inventoryCostUsdt` (all entities, DTOs, converters)

## Impact on PR #58 Features

| Feature | Impact | Action Required |
|:-------:|--------|-----------------|
| Binance/MexC sync | ✅ Still works | Synced transactions now use lazy model |
| Redis cache | ✅ Preserved | Price cache still used by `PricingFacade` |
| IOL integration | ✅ No change | Unaffected |
| Postman endpoints | ⚠️ Response changed | Field renamed: `stableTotalCost` → `inventoryCostUsdt` |

## API Consumers: Update Required
- All FE code accessing `.stableTotalCost` must change to `.inventoryCostUsdt`
- `/transaction/information/all/{portfolioName}` response schema changed

---