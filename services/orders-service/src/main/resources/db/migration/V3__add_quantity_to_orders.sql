-- Add quantity column for multi-unit purchases (default 1 for existing and new rows)
ALTER TABLE orders ADD COLUMN IF NOT EXISTS quantity INT NOT NULL DEFAULT 1;
