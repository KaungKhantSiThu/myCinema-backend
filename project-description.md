Project: Scalable Cinema Booking Service
Role: Backend Engineer

Status: Ready for Implementation

Goal: Build a thread-safe, transactional ticket booking system capable of handling high concurrency (multiple users trying to book the same seat simultaneously).

1. Technology Stack

Language: Java 21 (Use record, var, and modern switch statements).

Framework: Spring Boot 3.3+ (Web, Data JPA, Security, Validation).

Database: PostgreSQL 16 (Running in Docker).

Version Control: Flyway (For database schema migrations).

Concurrency: JPA Optimistic Locking (@Version).

Testing: JUnit 5 + Mockito (Aim for >80% coverage on Service layer).

2. Database Schema (The "Source of Truth")

This is the most critical section. Create your Flyway script V1__init_schema.sql based on this structure.

Explore

A. Static Data (The Cinema)

These tables change rarely.

halls

id (BigInt, PK)

name (Varchar) - e.g., "IMAX Hall 1"

total_rows (Int)

total_columns (Int)

seats (Represents the physical chair)

id (BigInt, PK)

hall_id (FK -> halls.id)

row_number (Int)

seat_number (Int)

Note: A generic "Seat A1" implies row 1, seat 1.

movies

id (BigInt, PK)

title (Varchar)

duration_minutes (Int)

genre (Varchar)

B. Dynamic Data (The Schedule)

These tables drive the application flow.

shows (A movie playing at a specific time)

id (BigInt, PK)

movie_id (FK -> movies.id)

hall_id (FK -> halls.id)

start_time (Timestamp)

end_time (Timestamp)

show_seats (The availability of a seat for a specific show)

id (BigInt, PK)

show_id (FK -> shows.id)

seat_id (FK -> seats.id)

status (Varchar/Enum) - Values: AVAILABLE, BOOKED, LOCKED (optional for checkout flow).

price (Decimal)

version (BigInt) - 🚨 CRITICAL: This is for Optimistic Locking. Default 0.

C. Transactional Data (The Users)

users

id (BigInt, PK)

email (Varchar, Unique)

password (Varchar) - Store BCrypt hash, never plain text.

roles (Varchar) - e.g., "ROLE_USER"

bookings

id (BigInt, PK)

user_id (FK -> users.id)

show_id (FK -> shows.id)

booking_time (Timestamp)

status (Varchar) - CONFIRMED, CANCELLED

3. API Specification (The Contract)

Implement these standard REST endpoints.

Public Endpoints (No Login Required)

GET /api/movies: List all movies currently showing.

GET /api/shows?movieId={id}&date={date}: List showtimes for a movie.

GET /api/shows/{showId}/seats:

Returns: A grid of seats.

Response Structure: Group seats by row so the frontend can draw the map easily. Include seatId, status, and price.

Protected Endpoints (Requires JWT "Bearer" Token)

POST /api/auth/register: Create a user.

POST /api/auth/login: Returns a JWT token.

POST /api/bookings: The complexity lives here.

Request Body:

JSON
{
"showId": 105,
"seatIds": [501, 502, 503]
}
Response: 201 Created (Booking ID) OR 409 Conflict (If seat taken).

4. Business Logic Requirements

R1: The "Double Booking" Prevention

Scenario: User A and User B try to book Seat #501 at the same time.

Mechanism: Use JPA @Version.

When the service loads Seat #501, it sees version = 1.

User A submits. Database checks: "Is version still 1?" -> Yes. Update to 2. Save.

User B submits. Database checks: "Is version still 1?" -> No (It is 2).

Action: Throw ObjectOptimisticLockingFailureException.

Result: Catch this exception in your GlobalExceptionHandler and return HTTP 409: "One or more selected seats were just booked by another user."

R2: Atomic Transactions

Booking multiple seats (e.g., A1, A2, A3) must be All or Nothing.

Annotate your Service method with @Transactional.

If A1 and A2 are free, but A3 fails the locking check, the entire transaction rolls back. A1 and A2 remain free.

R3: Input Validation

Check that seatIds actually belong to the showId.

Check that the show is in the future (cannot book past shows).

5. Implementation Phases (Your Roadmap)

Don't panic. Build it layer by layer.

Phase 1: The Skeleton (Days 1-2)

Initialize Spring Boot project (Web, JPA, Postgres, Flyway, Lombok).

Set up docker-compose.yml for PostgreSQL.

Write the SQL migration (V1__init.sql) for the schema above.

Run the app to ensure tables are created.

Phase 2: The Data Layer (Days 3-4)

Create JPA Entities (Movie, Show, ShowSeat...).

Crucial: Create a DataSeeder class (using CommandLineRunner) that runs on startup.

Create 1 Cinema Hall.

Create 10 Rows x 10 Seats (100 total).

Create 1 Movie ("Inception").

Create 1 Show.

Generate 100 ShowSeat records for that show (Status: AVAILABLE).

Phase 3: The Read API (Day 5)

Create MovieController and ShowController.

Implement GET /shows/{id}/seats.

Test with Postman. You should see a list of 100 seats.

Phase 4: The Booking Logic (The Hard Part) (Days 6-8)

Create BookingService.

Write the bookSeats method using @Transactional.

Simulate concurrency: Add a Thread.sleep(5000) inside the transaction, start the request in one tab, then try to book the same seat in another tab. Watch the 2nd one fail.

Phase 5: Security & Cleanup (Day 9+)

Add Spring Security.

Add JWT Filter.

Write Unit Tests for BookingService.