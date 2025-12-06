-- V2: Add transaction_id column to bookings table for payment tracking
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS transaction_id VARCHAR(255);

-- Create index for faster lookup by transaction_id
CREATE INDEX IF NOT EXISTS idx_bookings_transaction_id ON bookings(transaction_id);

