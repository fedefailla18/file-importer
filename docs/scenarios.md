# BDD Scenarios

## Feature: Transaction Ingestion and Holding Calculation

As an active trader, investor, or portfolio manager, I want to be able to track my holdings and calculate my portfolio value with full financial transparency.

I need to upload transaction sheets (or sync from Binance) and have the system compute:
- The exact holdings (quantity) per coin
- The cost basis per coin (how much I paid, in USDT terms)
- The realized profit or loss from each sell
- The unrealized profit or loss on current holdings
- The total portfolio value at today's prices
- A clear breakdown between **capital from my pocket** vs **capital recycled from profits**
- A "net position" answer to: "Did I make or lose money, and by how much?"

All stable coin amounts (USDT, USDC, BUSD, DAI, TUSD) are normalized to USDT for cost basis and P&L calculations. Exception: any stable coin that has suffered a collapse in value (e.g., USTC, USDN) must NOT be treated as equivalent to $1 — its actual market value at the time of sale must be used.

---

## Group A: Cost Basis and Holdings

### Scenario A1: Multiple BUY transactions build up cost basis via averaging
    Given a new portfolio "MyCrypto"
    When I upload the following transactions:
      | Date       | Symbol | Side | Price | Executed | Pair    | Paid With | Paid Amount |
      | 2023-01-01 | BTC    | BUY  | 20000 | 0.5      | BTCUSDT | USDT      | 10000       |
      | 2023-01-02 | BTC    | BUY  | 30000 | 0.5      | BTCUSDT | USDT      | 15000       |
    Then the holding for BTC in "MyCrypto" should be 1.0
    And the cost basis (stableTotalCost) for BTC should be 25000 USDT
    And the average buy price should be 25000 USDT
      # Calculation: (0.5 × 20000) + (0.5 × 30000) = 10000 + 15000 = 25000

### Scenario A2: Selling part of holdings reduces cost basis proportionally (profit)
    Given a portfolio "MyCrypto" with 1.0 BTC at cost basis 25000 USDT
    When I add a SELL transaction:
      | Date       | Symbol | Side | Price | Executed | Pair    | Paid With | Paid Amount |
      | 2023-02-01 | BTC    | SELL | 40000 | 0.4      | BTCUSDT | USDT      | 16000       |
    Then the BTC holding should be 0.6
    And the remaining cost basis should be 15000 USDT
      # avgCost = 25000 / 1.0 = 25000; costOfSold = 0.4 × 25000 = 10000; remaining = 25000 - 10000
    And the realized profit for BTC should be 6000 USDT
      # saleValue(16000) - costOfSold(10000) = 6000
    And the total capital invested across the portfolio should be 25000 USDT
    And the total capital recovered should be 16000 USDT

### Scenario A3: Selling at a loss reduces cost basis and records negative realized P&L
    Given a portfolio "MyCrypto" with 1.0 BTC at cost basis 60000 USDT
    When I add a SELL transaction:
      | Date       | Symbol | Side | Price | Executed | Pair    | Paid With | Paid Amount |
      | 2023-06-01 | BTC    | SELL | 20000 | 0.5      | BTCUSDT | USDT      | 10000       |
    Then the BTC holding should be 0.5
    And the remaining cost basis should be 30000 USDT
      # avgCost = 60000; costOfSold = 0.5 × 60000 = 30000; remaining = 30000
    And the realized profit for BTC should be -20000 USDT
      # saleValue(10000) - costOfSold(30000) = -20000 (realized loss)

### Scenario A4: Successive sells accumulate realized P&L
    Given a portfolio with 1.0 BTC at cost basis 30000 USDT
    When I sell 0.3 BTC at 40000 USDT (earning 12000, cost 9000 → profit +3000)
    And I sell another 0.3 BTC at 50000 USDT (earning 15000, cost 10500 → profit +4500)
      # After first sell: remaining 0.7 BTC, cost 21000, avgCost = 30000
      # Second sell cost: 0.3 × 30000 = 9000... wait:
      # After first sell: stableTotalCost = 30000 - (0.3×30000) = 21000, remaining = 0.7 BTC
      # avgCost still 30000 for the 0.7 remaining? No — avgCost = 21000/0.7 = 30000 (same, AVCO)
      # Second sell: cost = 0.3 × 30000 = 9000; sale = 0.3 × 50000 = 15000; profit = +6000
    Then total realized profit for BTC should be 9000 USDT
      # +3000 from first sell + +6000 from second sell = 9000
    And the BTC holding should be 0.4
    And the remaining cost basis should be 12000 USDT
      # 21000 - (0.3 × 30000) = 12000

---

## Group B: Cross-Asset Trades (Non-Stable Pairs)

### Scenario B1: Trading with BTC as quote reduces BTC holding and records realized profit
    Given a portfolio "MyCrypto" with 1.0 BTC at cost 30000 USDT and 0 ETH
    And ETH historical price at trade date is 1500 USDT
    When I add a transaction:
      | Date       | Symbol | Side | Price | Executed | Pair   | Paid With | Paid Amount |
      | 2023-03-01 | ETH    | BUY  | 0.05  | 10       | ETHBTC | BTC       | 0.5         |
    Then the ETH holding should be 10
    And the cost basis for ETH should be 7500 USDT
      # 10 ETH × 1500 USDT/ETH = 7500 USDT (using historical price)
    And the BTC holding should be 0.5
    And the BTC realized profit should reflect a partial BTC sale at market price
      # 0.5 BTC "sold" at 15000 USDT/BTC (= 1500 / 0.05) — BTC realized P&L updated
    And the cost basis for BTC should decrease accordingly

### Scenario B2: Quote asset cost basis updates correctly on reverse trade
    Given the portfolio from Scenario B1
    When I sell 5 ETH back to BTC at 0.05 BTC/ETH (ETH at 1500 USDT)
    Then ETH holding should be 5
    And ETH realized profit should reflect half the ETH position sold at cost (break even)
    And the BTC holding should increase by 0.25

---

## Group C: Capital Tracking — Cash from Pocket vs Recycled Profit

This is the key financial transparency feature. The system must distinguish between:
- **Invested Capital** — USDT/stables you originally deposited to buy crypto
- **Recovered Capital** — USDT/stables you received from selling crypto
- **Net Capital from Pocket** — Invested - Recovered = real new money you needed

### Scenario C1: Basic capital tracking on single buy
    Given a new portfolio "Tracker"
    When I buy 1.0 BTC at 40000 USDT
    Then totalBuySpentUsdt should be 40000
    And totalSellEarnedUsdt should be 0
    And net capital from pocket = 40000 - 0 = 40000 USDT
    And no profit has been reinvested yet

### Scenario C2: Sell, then reinvest — recycled capital tracked
    Given portfolio "Tracker" with 1.0 BTC at cost 40000 USDT
    When I sell 0.5 BTC at 50000 USDT (received 25000 USDT)
    And I buy 500 units of ETH at 25 USDT each (spent 12500 USDT)
    Then totalBuySpentUsdt should be 40000 + 12500 = 52500 USDT
    And totalSellEarnedUsdt should be 25000 USDT
    And net capital from pocket = 52500 - 25000 = 27500 USDT
      # Interpretation: I only needed 27500 real cash; the rest came from selling BTC
    And profit reinvested = min(sellEarned, newBuys) = 12500 USDT reinvested from proceeds
    And realized profit for BTC = 25000 - (0.5 × 40000) = 5000 USDT (confirmed profit from sell)

### Scenario C3: Full exit and reinvestment — pocket money stays constant
    Given portfolio "Tracker" starting with 10000 USDT invested in BTC
    When BTC doubles and I sell all for 20000 USDT
    And I reinvest all 20000 USDT into ETH
    Then totalBuySpentUsdt should be 30000 USDT (10000 + 20000)
    And totalSellEarnedUsdt should be 20000 USDT
    And net capital from pocket = 30000 - 20000 = 10000 USDT
      # Correct: I only ever put 10000 from my pocket. The rest was profit recycled.
    And totalRealizedProfit for BTC should be 10000 USDT

---

## Group D: "How Much Money Do I Have?" — The Net Worth View

This is the answer to: "If I liquidated everything today, would I be up or down vs. what I put in?"

### Scenario D1: Calculating net position at a moment in time
    Given portfolio "MyFund" with:
      - 10000 USDT invested from pocket (net capital)
      - Realized profit accumulated: 3000 USDT
      - Current portfolio market value: 15000 USDT
    Then the total value I have = current portfolio value + cash recovered - original investment
      # = 15000 (in portfolio) + 3000 (already taken out as profit) - 10000 (original)
      # = 8000 USDT net gain
    And if I had just kept 10000 USDT, I'd have 10000 USDT
    So the portfolio has outperformed holding cash by 8000 USDT

    The formula:
      Net Return = (currentPortfolioValue + totalSellEarnedUsdt) - totalBuySpentUsdt
      Positive = portfolio is up overall
      Negative = portfolio is down overall

### Scenario D2: Tracking "money I should have today"
    Given a portfolio where I invested 20000 USDT total from pocket
    And I've sold some positions, recovering 8000 USDT in realized gains
    And my current holdings are worth 18000 USDT at market prices
    Then:
      - "Money in portfolio today" = 18000 USDT (current market value)
      - "Money I've taken out as profit" = 8000 USDT (cumulative sell proceeds - cost of sold)
        # Wait: this is realized profit, not total proceeds.
        # Realized profit = totalSellEarnedUsdt - sum of cost of sold units
      - "Total economic value" = currentPortfolioValue + totalRealizedProfitUsdt
        # = 18000 + realized P&L total
      - "vs. capital from pocket (20000)" → tells me if I'm up or down

### Scenario D3: Break-even detection
    Given I invested 10000 USDT across multiple coins
    And I've realized 2000 USDT in losses
    And current portfolio market value is 8000 USDT
    Then total value (8000 + realized gains/losses) = 8000 - 2000 = 6000 USDT effective
    And I am down 4000 USDT vs. my initial 10000 USDT
    And "money from pocket still at risk" = 10000 - proceeds already recovered

---

## Group E: Unrealized Profit and Total P&L

### Scenario E1: Unrealized profit on an open position
    Given I hold 1.0 BTC with cost basis 40000 USDT
    And current BTC price is 60000 USDT
    Then current position value = 60000 USDT
    And unrealized profit = 60000 - 40000 = 20000 USDT
    And if I sell at this price, my total realized P&L would go up by 20000 USDT

### Scenario E2: Combined realized and unrealized P&L per coin
    Given I bought 2.0 BTC at 30000 USDT each (cost basis = 60000)
    When I sell 1.0 BTC at 45000 USDT (realized profit = 15000 USDT)
    And BTC is now at 50000 USDT
    Then:
      - Realized P&L = 15000 USDT (confirmed)
      - Remaining BTC holding = 1.0 BTC
      - Remaining cost basis = 30000 USDT
      - Unrealized P&L = (1.0 × 50000) - 30000 = 20000 USDT
      - Total P&L (realized + unrealized) = 15000 + 20000 = 35000 USDT

### Scenario E3: Portfolio-level total P&L
    Given a portfolio with multiple coins all with both realized and unrealized P&L
    Then total portfolio P&L = sum of (realized + unrealized) across all holdings
    And this total should reflect whether the portfolio is net positive or negative

---

## Group F: "How Much Could I Have Earned?" — Opportunity Cost

This answers: "If I had held my sold positions instead of selling, what would they be worth today?"

### Scenario F1: Opportunity cost of selling too early
    Given I sold 1.0 BTC at 45000 USDT (realized profit = 5000 USDT, cost basis was 40000)
    And BTC is now at 70000 USDT
    Then missed gain = (70000 - 45000) × 1.0 = 25000 USDT
      # I earned 5000 by selling, but left 25000 on the table
    And total theoretical value if held = 70000 USDT (vs 45000 received)
    And opportunity cost = 25000 USDT

    Note: This feature requires storing "quantity sold" and "sell price" per trade.
    It is NOT the same as realized P&L — realized P&L compares sale vs cost,
    opportunity cost compares sale vs current market.

### Scenario F2: No opportunity cost when sell price > current price
    Given I sold 1.0 BTC at 65000 USDT
    And BTC is now at 50000 USDT
    Then opportunity cost = max(0, (50000 - 65000) × 1.0) = 0 (no missed gains; the sell was optimal)
    And I actually "saved" 15000 USDT by selling when I did

---

## Group G: Portfolio Valuation

### Scenario G1: Total portfolio value at current market prices
    Given a portfolio with 0.5 BTC at market price 50000 USDT and 10 ETH at 3000 USDT
    Then total portfolio value = (0.5 × 50000) + (10 × 3000) = 25000 + 30000 = 55000 USDT
    And total portfolio value in BTC = 55000 / 50000 = 1.1 BTC

### Scenario G2: Portfolio distribution (allocation percentages)
    Given the portfolio from G1 (55000 USDT total)
    Then BTC allocation = 25000 / 55000 = 45.45%
    And ETH allocation = 30000 / 55000 = 54.55%
    And all percentages sum to 100%

### Scenario G3: Portfolio distribution with buy and sell totals
    Given a portfolio where I invested 40000 USDT total across buys
    And I've received 15000 USDT from sells
    Then totalBuySpentUsdt = 40000
    And totalSellEarnedUsdt = 15000
    And net capital invested = 25000 USDT
    And these figures are visible alongside the distribution

---

## Group H: Fee Handling

### Scenario H1: Fee in quote currency (USDT) is added to cost basis
    Given a BUY transaction: 1.0 BTC at 50000 USDT with a 100 USDT fee (paid in USDT)
    When the transaction is processed
    Then the cost basis for BTC should be 50100 USDT
      # The fee increases what you effectively paid per coin

### Scenario H2: Fee in third currency (BNB) is tracked separately
    Given a BUY transaction: 100 FET at 1 USDT with a 0.01 BNB fee (fee ≠ quote currency)
    When the transaction is processed
    Then the FET cost basis should be 100 USDT (fee NOT added, different currency)
    And the fee of 0.01 BNB is recorded on the transaction for reference
    And if BNB has a holding, it should NOT automatically decrease by 0.01
      # BNB fees are deducted by Binance directly; we record them for tax but don't simulate

### Scenario H3: Zero-fee transaction is handled normally
    Given a BUY with no fee specified
    When processed
    Then cost basis = executed × price (no fee added)

---

## Group I: Stable Coin Normalization

### Scenario I1: USDC paid-with is treated as USDT
    Given a BUY transaction: 1.0 BTC at 50000, paid with 50000 USDC
    When processed
    Then cost basis should be 50000 USDT
    And USDC balance should NOT create a separate holding for cost-basis purposes

### Scenario I2: BUSD, DAI, TUSD treated as $1 stable
    Given BUY transactions paid in BUSD, DAI, and TUSD respectively
    When processed
    Then all cost bases are calculated as if paid with USDT (1:1 USD parity)

### Scenario I3: USTC (crashed stable) NOT treated as $1
    Given a SELL transaction receiving 1000 USTC as proceeds
    When USTC is trading at $0.01 (post-depeg crash)
    Then the realized proceeds value should be 1000 × 0.01 = 10 USDT
    And NOT 1000 USDT (which would overstate the realized gain)

---

## Group J: Safety Constraints

### Scenario J1: Overselling prevention (negative holding guard)
    Given a portfolio with 0.5 BTC at cost 15000 USDT
    When trying to SELL 1.0 BTC (more than held)
    Then the system should log a warning
    And reduce BTC holding to 0.0 (not negative)
    And calculate realized profit based on the available 0.5 BTC only

### Scenario J2: Empty portfolio sell is a no-op
    Given a portfolio with no BTC holdings
    When a SELL 0.5 BTC transaction arrives
    Then realized profit should be 0 (nothing to compute cost basis against)
    And the holding should remain 0 (not go negative)

---

## Group K: Multi-Portfolio Isolation

### Scenario K1: Same coin in two portfolios has independent holdings
    Given portfolio "InvestorA" bought 1.0 BTC at 40000 USDT
    And portfolio "InvestorB" bought 1.0 BTC at 30000 USDT
    Then InvestorA cost basis for BTC = 40000 USDT
    And InvestorB cost basis for BTC = 30000 USDT
    And selling in InvestorA does NOT affect InvestorB holdings

### Scenario K2: Portfolio totals are portfolio-scoped
    Given InvestorA with totalBuySpentUsdt = 40000
    And InvestorB with totalBuySpentUsdt = 30000
    Then fetching InvestorA distribution returns 40000, not 70000

---

## Tech Lead Analysis: Current Flow Completeness

### Flow Under Review
```
Upload / Sync → TransactionProcessor.process() → Holding update
    → CoinInformationService → CoinInformationFacade
    → PortfolioDistributionFacade → PortfolioDistribution response
```

### ✅ What Is Working (Confirmed by Code + Integration Tests)

| # | Scenario | Covered by Test |
|---|----------|----------------|
| A1 | Cost basis accumulation on multiple BUYs | EndToEndScenariosIntegrationSpec "Scenario 1 & 2" |
| A2 | Sell reduces holding + cost basis proportionally | EndToEndScenariosIntegrationSpec "Scenario 1 & 2" |
| B1 | Cross-asset trade (ETH/BTC) updates both holdings | EndToEndScenariosIntegrationSpec "Scenario 3" |
| G1 | Portfolio valuation at market prices | EndToEndScenariosIntegrationSpec "Scenario 4" |
| G2 | Portfolio allocation percentages | EndToEndScenariosIntegrationSpec "Scenario 4" |
| K1 | Multi-portfolio isolation | EndToEndScenariosIntegrationSpec "Multi-portfolio isolation" |
| File upload | Binance CSV ingestion | EndToEndScenariosIntegrationSpec "Uploading Binance CSV" |

### ❌ Gaps — Not Covered by Current Tests

| # | Scenario | Gap |
|---|----------|-----|
| A3 | Successive sells accumulate realized P&L correctly | No test for cumulative P&L across multiple sell events |
| C1–C3 | Capital from pocket vs recycled profit | `totalBuySpentUsdt` and `totalSellEarnedUsdt` are never asserted in integration tests |
| D1–D3 | Net worth / "money you should have today" | No test verifies the combined realized+current value calculation |
| E1–E3 | Unrealized P&L tracking | `unrealizedProfit` computed but never asserted in integration tests |
| F1–F2 | Opportunity cost / "could've earned" | No field exists in the data model; feature not implemented |
| H1–H3 | Fee handling impact on cost basis | Fee is stored on Transaction entity but NOT added to `stableTotalCost` in TransactionProcessor |
| I1–I3 | Stable coin normalization | USDC/BUSD treated as USDT? The code has `STABLE` list but integration test never exercises paid-with USDC |
| I3 | USTC crash protection | No USTC-specific logic found in codebase |
| J1 | Overselling guard | Test is "Planned" in original doc but NOT in EndToEndScenariosIntegrationSpec |
| J2 | Empty portfolio sell is no-op | Not tested |
| A3 | Loss scenario (Scenario 2b in original) | The realized loss case is defined in original doc but NOT in EndToEndScenariosIntegrationSpec |

### ⚠️ Known Implementation Gaps vs. PO Requirements

1. **Fee NOT included in cost basis** (Scenario H1): `TransactionProcessor` stores `feeAmount` on the `Transaction` but does NOT add it to `stableTotalCost`. This means cost basis is understated for fee-bearing trades.

2. **No opportunity cost calculation** (Group F): There is no concept of "what could I have earned if I hadn't sold." This would require storing per-trade sold-quantity × sell-price and comparing with current market price. **This is a product gap, not a data model bug.**

3. **Capital sourcing is approximate** (Group C): `totalBuySpentUsdt` and `totalSellEarnedUsdt` exist on `PortfolioDistribution` but net capital from pocket (`buys - sells`) is not a first-class computed field. The frontend must compute it as `totalBuySpentUsdt - totalSellEarnedUsdt`.

4. **`unrealizedProfit` is ephemeral**: Computed in `CoinInformationService` at query time from current market price, but NOT persisted on the `Holding` entity. If the pricing API is slow or fails, the UI gets stale/zero unrealized P&L.

5. **UST is in the STABLE list — this is a live bug**: `OperationUtils.STABLE` contains `"UST"`, which is the Terra stablecoin that collapsed in May 2022 (later renamed USTC). Any trade received in UST after the depeg is being valued at $1 instead of its actual market price (~$0.01), overstating realized gains by ~100×. This needs to be removed from the STABLE list or made conditional on trade date.

6. **Total portfolio P&L is not a single response field**: The frontend must combine `totalRealizedProfitUsdt` (sum across coins from `CoinInformationResponse`) + `unrealizedProfit` (sum across coins) to display total P&L. There is no pre-computed `totalPortfolioPnl` field.
