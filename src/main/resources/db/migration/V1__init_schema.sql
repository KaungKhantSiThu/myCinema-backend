-- V1: Initialize Cinema Booking Schema
-- This migration creates all tables required for the cinema booking system

-- A. Static Data (The Cinema)

-- halls: Represents physical cinema halls
CREATE TABLE halls (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    total_rows INT NOT NULL,
    total_columns INT NOT NULL,
    CONSTRAINT chk_rows_positive CHECK (total_rows > 0),
    CONSTRAINT chk_columns_positive CHECK (total_columns > 0)
);

-- seats: Represents physical seats in halls
CREATE TABLE seats (
    id BIGSERIAL PRIMARY KEY,
    hall_id BIGINT NOT NULL,
    row_number INT NOT NULL,
    seat_number INT NOT NULL,
    CONSTRAINT fk_seats_hall FOREIGN KEY (hall_id) REFERENCES halls(id) ON DELETE CASCADE,
    CONSTRAINT chk_row_positive CHECK (row_number > 0),
    CONSTRAINT chk_seat_positive CHECK (seat_number > 0),
    UNIQUE (hall_id, row_number, seat_number)
);

-- movies: Represents movies available for screening
CREATE TABLE movies (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    duration_minutes INT NOT NULL,
    genre VARCHAR(50) NOT NULL,
    CONSTRAINT chk_duration_positive CHECK (duration_minutes > 0)
);

-- B. Dynamic Data (The Schedule)

-- shows: A movie playing at a specific time in a hall
CREATE TABLE shows (
    id BIGSERIAL PRIMARY KEY,
    movie_id BIGINT NOT NULL,
    hall_id BIGINT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    CONSTRAINT fk_shows_movie FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE,
    CONSTRAINT fk_shows_hall FOREIGN KEY (hall_id) REFERENCES halls(id) ON DELETE CASCADE,
    CONSTRAINT chk_show_time_valid CHECK (end_time > start_time)
);

-- show_seats: The availability of a seat for a specific show (CRITICAL for concurrency)
CREATE TABLE show_seats (
    id BIGSERIAL PRIMARY KEY,
    show_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    price DECIMAL(10, 2) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0, -- CRITICAL: Optimistic Locking
    CONSTRAINT fk_show_seats_show FOREIGN KEY (show_id) REFERENCES shows(id) ON DELETE CASCADE,
    CONSTRAINT fk_show_seats_seat FOREIGN KEY (seat_id) REFERENCES seats(id) ON DELETE CASCADE,
    CONSTRAINT chk_status_valid CHECK (status IN ('AVAILABLE', 'BOOKED', 'LOCKED')),
    CONSTRAINT chk_price_positive CHECK (price > 0),
    UNIQUE (show_id, seat_id)
);

-- C. Transactional Data (The Users)

-- users: Represents registered users
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL, -- BCrypt hash
    roles VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- bookings: Represents confirmed bookings
CREATE TABLE bookings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    show_id BIGINT NOT NULL,
    booking_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    total_amount DECIMAL(10, 2) NOT NULL,
    CONSTRAINT fk_bookings_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_bookings_show FOREIGN KEY (show_id) REFERENCES shows(id) ON DELETE CASCADE,
    CONSTRAINT chk_booking_status_valid CHECK (status IN ('CONFIRMED', 'CANCELLED')),
    CONSTRAINT chk_total_amount_positive CHECK (total_amount > 0)
);

-- booking_seats: Junction table linking bookings to show_seats
CREATE TABLE booking_seats (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    show_seat_id BIGINT NOT NULL,
    CONSTRAINT fk_booking_seats_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_seats_show_seat FOREIGN KEY (show_seat_id) REFERENCES show_seats(id) ON DELETE CASCADE,
    UNIQUE (booking_id, show_seat_id)
);

-- Indexes for performance
CREATE INDEX idx_shows_movie_id ON shows(movie_id);
CREATE INDEX idx_shows_hall_id ON shows(hall_id);
CREATE INDEX idx_shows_start_time ON shows(start_time);
CREATE INDEX idx_show_seats_show_id ON show_seats(show_id);
CREATE INDEX idx_show_seats_status ON show_seats(status);
CREATE INDEX idx_bookings_user_id ON bookings(user_id);
CREATE INDEX idx_bookings_show_id ON bookings(show_id);
CREATE INDEX idx_users_email ON users(email);

