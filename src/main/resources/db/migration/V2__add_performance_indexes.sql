-- V2__add_performance_indexes.sql
-- Add indexes for frequently queried columns to improve query performance

-- Index on show_seats for lookups by show_id (used in seat availability queries)
CREATE INDEX IF NOT EXISTS idx_show_seats_show_id ON show_seats(show_id);

-- Index on show_seats for lookups by status (used in availability filters)
CREATE INDEX IF NOT EXISTS idx_show_seats_status ON show_seats(status);

-- Index on bookings for user lookups (used in getUserBookings)
CREATE INDEX IF NOT EXISTS idx_bookings_user_id ON bookings(user_id);

-- Composite index on bookings for user + time ordering
CREATE INDEX IF NOT EXISTS idx_bookings_user_time ON bookings(user_id, booking_time DESC);

-- Index on shows for movie lookups (used in getShowsByMovie)
CREATE INDEX IF NOT EXISTS idx_shows_movie_id ON shows(movie_id);

-- Index on shows for start_time queries (used in upcoming shows)
CREATE INDEX IF NOT EXISTS idx_shows_start_time ON shows(start_time);

-- Composite index on shows for movie + date queries
CREATE INDEX IF NOT EXISTS idx_shows_movie_date ON shows(movie_id, start_time);

-- Index on booking_seats for booking lookups
CREATE INDEX IF NOT EXISTS idx_booking_seats_booking_id ON booking_seats(booking_id);

-- Index on users email for authentication lookups
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- Comments for documentation
COMMENT ON INDEX idx_show_seats_show_id IS 'Optimizes seat availability queries by show';
COMMENT ON INDEX idx_bookings_user_time IS 'Optimizes user booking history queries with time ordering';
COMMENT ON INDEX idx_shows_start_time IS 'Optimizes upcoming shows queries';

