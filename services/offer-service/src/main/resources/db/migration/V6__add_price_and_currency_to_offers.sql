-- Add price and currency to offers (nullable for existing rows; app defaults to 499 cents, USD)
ALTER TABLE offers
    ADD COLUMN price_cents INTEGER NULL,
    ADD COLUMN currency VARCHAR(3) NULL;

-- Optional: backfill existing rows with default sell price
UPDATE offers SET price_cents = 499, currency = 'USD' WHERE price_cents IS NULL;
