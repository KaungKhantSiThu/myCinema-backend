-- V4: Add name column to users table
ALTER TABLE users ADD COLUMN name VARCHAR(100);

-- Set default name for existing users (using email prefix before @)
UPDATE users SET name = 'User' WHERE name IS NULL;

