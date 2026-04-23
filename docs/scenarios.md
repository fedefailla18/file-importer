# BDD Scenarios

## Feature: Transaction Ingestion and Holding Calculation

### Scenario 1: Uploading multiple BUY transactions for a stable pair
    Given a new portfolio "MyCrypto"
    When I upload a file with the following transactions:
      | Date       | Symbol | Side | Price | Executed | Pair     | Paid With | Paid Amount |
      | 2023-01-01 | BTC    | BUY  | 20000 | 0.5      | BTCUSDT  | USDT      | 10000       |
      | 2023-01-02 | BTC    | BUY  | 30000 | 0.5      | BTCUSDT  | USDT      | 15000       |
    Then the holding for BTC in "MyCrypto" should be 1.0
    And the total cost for BTC should be 25000 USDT
    And the average buy price should be 25000 USDT

### Scenario 2: Selling part of the holdings (Realizing Profit/Loss)
    Given a portfolio "MyCrypto" with 1.0 BTC at a cost of 25000 USDT
    When I add a transaction:
      | Date       | Symbol | Side | Price | Executed | Pair     | Paid With | Paid Amount |
      | 2023-02-01 | BTC    | SELL | 40000 | 0.4      | BTCUSDT  | USDT      | 16000       |
    Then the holding for BTC should be 0.6
    And the realized profit should be 6000 USDT (Calculation: 16000 - (0.4 * 25000))
    And the remaining cost basis should be 15000 USDT (0.6 * 25000)

### Scenario 2b: Realizing a Loss (Market Downturn)
    Given a portfolio "MyCrypto" with 1.0 BTC at a cost of 60000 USDT (bought near ATH)
    When I add a transaction:
      | Date       | Symbol | Side | Price | Executed | Pair     | Paid With | Paid Amount |
      | 2023-06-01 | BTC    | SELL | 20000 | 0.5      | BTCUSDT  | USDT      | 10000       |
    Then the holding for BTC should be 0.5
    And the realized profit should be -20000 USDT (Realized Loss)
    And the remaining cost basis should be 30000 USDT

### Scenario 3: Trading between non-stable coins
    Given a portfolio "MyCrypto" with 1.0 BTC and 0 ETH
    When I add a transaction:
      | Date       | Symbol | Side | Price | Executed | Pair     | Paid With | Paid Amount |
      | 2023-03-01 | ETH    | BUY  | 0.05  | 10       | ETHBTC   | BTC       | 0.5         |
    Then the holding for BTC should decrease by 0.5 (to 0.5)
    And the holding for ETH should be 10
    And the cost basis for ETH should be 0.5 BTC (converted to USDT at the time of trade)

### Scenario 4: Portfolio valuation
    Given a portfolio with 0.5 BTC and 10 ETH
    And the current market price is 50000 USDT for BTC and 3000 USDT for ETH
    Then the total portfolio value should be (0.5 * 50000) + (10 * 3000) = 55000 USDT

## Scenarios to Consolidate

### Scenario 5: Handling Fees in Cost Basis (Planned)
    Given a transaction BUY 1 BTC at 50000 USDT with 100 USDT fee
    When processing the transaction
    Then the total cost basis for the BTC holding should be 50100 USDT

### Scenario 6: Negative Holding Prevention (Planned)
    Given a portfolio with 0.5 BTC
    When trying to SELL 1.0 BTC
    Then the system should log a warning and only decrease holding to 0.0 (preventing negative balances)
    And the realized profit should be calculated based on the available 0.5 BTC
