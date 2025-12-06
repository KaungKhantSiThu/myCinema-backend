-- V3: Add seat_holds table for temporary seat reservations during checkout
-- This enables the hold-then-confirm booking flow

CREATE TABLE seat_holds (
    id BIGSERIAL PRIMARY KEY,
    hold_token VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    show_id BIGINT NOT NULL,
    seat_ids VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_seat_holds_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_seat_holds_show FOREIGN KEY (show_id) REFERENCES shows(id) ON DELETE CASCADE,
    CONSTRAINT chk_hold_status_valid CHECK (status IN ('ACTIVE', 'CONFIRMED', 'RELEASED', 'EXPIRED'))
);

-- Index for finding holds by token (primary lookup)
CREATE INDEX idx_seat_holds_token ON seat_holds(hold_token);

-- Index for finding expired holds (cleanup job)
CREATE INDEX idx_seat_holds_status_expires ON seat_holds(status, expires_at);

-- Index for finding holds by user
CREATE INDEX idx_seat_holds_user ON seat_holds(user_id);

-- Index for finding holds by show
CREATE INDEX idx_seat_holds_show ON seat_holds(show_id);

-- Add locked_until column to show_seats for tracking hold expiration
ALTER TABLE show_seats ADD COLUMN locked_until TIMESTAMP;

ALTER TABLE show_seats ADD COLUMN locked_by_user_id BIGINT;

-- Index for finding locked seats (H2 compatible - no partial index)
CREATE INDEX idx_show_seats_locked_until ON show_seats(locked_until);
