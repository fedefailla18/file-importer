-- Add exchange source metadata to portfolios so UI can render exchange-specific actions reliably.
ALTER TABLE file_importer_schema.portfolio
    ADD COLUMN IF NOT EXISTS exchange_name VARCHAR(20);

-- Backfill known default exchange portfolios.
UPDATE file_importer_schema.portfolio
SET exchange_name = 'BINANCE'
WHERE upper(name) = 'BINANCE'
  AND exchange_name IS NULL;

UPDATE file_importer_schema.portfolio
SET exchange_name = 'MEXC'
WHERE upper(name) = 'MEXC'
  AND exchange_name IS NULL;
