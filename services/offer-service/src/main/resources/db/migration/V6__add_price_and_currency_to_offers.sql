ALTER TABLE offers
    ADD COLUMN price_cents INTEGER NULL,
    ADD COLUMN currency VARCHAR(3) NULL;

UPDATE offers SET price_cents = 499, currency = 'USD' WHERE price_cents IS NULL;
