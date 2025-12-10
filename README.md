# Cinema Booking Service

A scalable, thread-safe cinema ticket booking system built with Spring Boot 3.3+ that handles high concurrency using JPA Optimistic Locking.

## 🚀 Production Ready (December 2025)

✅ **Java 21 LTS** - Upgraded with modern features  
✅ **All Tests Passing** - 59/59 tests (100%)  
✅ **TMDb Integration** - Movie import from The Movie Database  
✅ **Security Hardened** - Environment-based configuration, JWT auth, rate limiting  
✅ **CI/CD Pipeline** - GitHub Actions with automated testing & Docker builds  
✅ **Comprehensive Docs** - Deployment guide, API docs, troubleshooting  

📊 **Production Readiness: 90%** - TMDb integration complete, comprehensive testing in progress

### 📚 Quick Links
- 🎬 [**STARTUP_GUIDE.md**](STARTUP_GUIDE.md) - **START HERE!** Complete setup & startup guide
- 🎯 [**ACTION_SUMMARY.md**](ACTION_SUMMARY.md) - Quick action guide
- 🎥 [**TMDB_INTEGRATION.md**](TMDB_INTEGRATION.md) - TMDb movie import feature
- 📝 [**IMPLEMENTATION_SUMMARY.md**](IMPLEMENTATION_SUMMARY.md) - Recent implementation details
- 🔧 [**RECOMMENDATIONS.md**](RECOMMENDATIONS.md) - Full production readiness guide
- 🚀 [**DEPLOYMENT.md**](DEPLOYMENT.md) - Step-by-step deployment instructions
- 📊 [**IMPLEMENTATION_ANALYSIS.md**](IMPLEMENTATION_ANALYSIS.md) - Design patterns & algorithms deep dive

### 🏃 Quick Start
```bash
./start.sh  # One-command startup (PostgreSQL + Application)
```

## ⚡ Recent Improvements (December 2025)

🎉 **Latest Enhancements:**
- ✅ **Java 21 LTS Upgrade** - Upgraded from Java 17 to Java 21 with full compatibility
- ✅ **TMDb Integration** - Admin movie import from The Movie Database API
  - Search movies from TMDb catalog
  - Import movies with automatic metadata (title, runtime, genres, poster, etc.)
  - Modular architecture using adapter pattern for replaceability
  - Conditional configuration (@ConditionalOnProperty) for optional enablement
- ✅ **Test Suite Enhanced** - All 59 tests passing with 100% success rate
- ✅ **Production Security** - Environment variables, custom exceptions, rate limiting
- ✅ **CI/CD Pipeline** - GitHub Actions workflow with JDK 21, Docker builds, and security scanning
- ✅ **Comprehensive Documentation** - Startup guide, TMDb integration guide, troubleshooting

---

## 🎯 Project Overview

This project demonstrates professional backend engineering practices for handling concurrent bookings in a cinema system. The core challenge: **preventing double-booking when multiple users try to book the same seat simultaneously**.

## 🚀 Technology Stack

- **Java 21 LTS** - Modern Java features (records, virtual threads, pattern matching)
- **Spring Boot 3.3.5** - Web, Data JPA, Security, Validation
- **PostgreSQL 16** - Running in Docker
- **Flyway** - Database migration management
- **JWT (jjwt 0.12.5)** - Stateless authentication
- **JPA Optimistic Locking** - Concurrency control with `@Version`
- **TMDb API** - Movie metadata integration via Java wrapper
- **Lombok** - Boilerplate reduction
- **JUnit 5 + Mockito** - Unit testing with H2 in-memory database

## 🏗️ Architecture

### Database Schema

```
Cinema Structure:
- halls (cinema halls)
- seats (physical seats in halls)
- movies (available movies)

Show Schedule:
- shows (movie screenings)
- show_seats (seat availability per show) ⚠️ CRITICAL: Uses @Version for concurrency

User & Bookings:
- users (registered users)
- bookings (confirmed bookings)
- booking_seats (junction table)
```

### Key Design Patterns

1. **Optimistic Locking**: Prevents double-booking without database locks
2. **Transactional Integrity**: All-or-nothing booking (multiple seats)
3. **JWT Stateless Auth**: Scalable authentication
4. **DTO Pattern**: Clean separation between entities and API responses
5. **Adapter Pattern**: TMDb integration with replaceable data sources (MovieDataSource interface)
6. **Conditional Configuration**: Optional features via @ConditionalOnProperty

## 🔧 Setup & Installation

### Prerequisites

- Java 21 LTS ✅
- Docker & Docker Compose
- Maven 3.8+ (included via wrapper)
- TMDb API Key (for movie import feature)

### Quick Start

**Option 1: One-Command Startup** (Recommended)
```bash
./start.sh
```

**Option 2: Manual Setup**

1. **Start PostgreSQL**
```bash
docker-compose up -d
```

2. **Set TMDb API Key** (Optional - for movie import feature)
```bash
export TMDB_API_KEY="your_api_key_here"
```

3. **Start Application**
```bash
./mvnw spring-boot:run
```

4. **Access Application**
- Swagger UI: http://localhost:8080/swagger-ui.html
- API Base: http://localhost:8080/api
- Health Check: http://localhost:8080/actuator/health

### Detailed Setup Guide

For complete setup instructions, troubleshooting, and testing guide, see **[STARTUP_GUIDE.md](STARTUP_GUIDE.md)**

3. **Build the project**
```bash
mvn clean install
```

4. **Run the application**
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### Database Initialization

On first startup:
- Flyway automatically creates all tables
- DataSeeder populates:
  - 1 IMAX Hall (10x10 = 100 seats)
  - 1 Movie ("Inception")
  - 1 Show (starting 2 hours from startup)
  - 100 ShowSeats (all AVAILABLE)

## 📡 API Endpoints

### Public Endpoints (No Authentication)

#### Get All Movies
```http
GET /api/movies
```

#### Get Shows
```http
GET /api/shows?movieId={id}&date={YYYY-MM-DD}
```

#### Get Seats for a Show
```http
GET /api/shows/{showId}/seats
```

Response:
```json
{
  "showId": 1,
  "movieTitle": "Inception",
  "seatsByRow": {
    "1": [
      {"seatId": 1, "rowNumber": 1, "seatNumber": 1, "status": "AVAILABLE", "price": 15.00},
      {"seatId": 2, "rowNumber": 1, "seatNumber": 2, "status": "AVAILABLE", "price": 15.00}
    ]
  }
}
```

### Authentication Endpoints

#### Register
```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "user@example.com",
  "message": "Login successful"
}
```

### Protected Endpoints (Requires JWT)

#### Book Seats
```http
POST /api/bookings
Authorization: Bearer {token}
Content-Type: application/json

{
  "showId": 1,
  "seatIds": [1, 2, 3]
}
```

Success Response (201):
```json
{
  "bookingId": 1,
  "showId": 1,
  "movieTitle": "Inception",
  "showTime": "2024-03-20T20:00:00",
  "seats": [
    {"rowNumber": 1, "seatNumber": 1, "price": 15.00},
    {"rowNumber": 1, "seatNumber": 2, "price": 15.00},
    {"rowNumber": 1, "seatNumber": 3, "price": 15.00}
  ],
  "totalAmount": 45.00,
  "bookingTime": "2024-03-20T18:30:00",
  "status": "CONFIRMED"
}
```

Conflict Response (409):
```json
{
  "status": 409,
  "message": "One or more selected seats were just booked by another user. Please refresh and try again.",
  "timestamp": "2024-03-20T18:30:00"
}
```

#### Get My Bookings
```http
GET /api/bookings/my-bookings
Authorization: Bearer {token}
```

## 🔐 Security

### Password Storage
- Passwords are hashed using BCrypt (never stored in plain text)

### JWT Authentication
- Tokens expire after 24 hours (configurable in `application.properties`)
- Stateless authentication (no server-side sessions)

### Authorization
- Public: Movie and show listings
- Protected: Booking operations

## 🎯 Concurrency Control - The Core Challenge

### The Problem
```
Time    User A                  User B
----    ------                  ------
T1      Sees Seat #1 AVAILABLE  Sees Seat #1 AVAILABLE
T2      Clicks "Book"           Clicks "Book"
T3      ❌ Both succeed → Double booking!
```

### The Solution: Optimistic Locking

1. **@Version field** in `ShowSeat` entity
2. When User A loads seat: `version = 1`
3. When User A saves: DB checks `WHERE version = 1` → Success, updates to `version = 2`
4. When User B saves: DB checks `WHERE version = 1` → **FAIL** (version is now 2)
5. Throws `ObjectOptimisticLockingFailureException`
6. GlobalExceptionHandler returns HTTP 409 Conflict

### Testing Concurrency

Uncomment the `Thread.sleep(5000)` in `BookingService.bookSeats()`:

1. Start booking in Browser Tab 1
2. Immediately start booking same seats in Tab 2
3. Tab 1 succeeds (201 Created)
4. Tab 2 fails (409 Conflict)

## 🧪 Testing

### Run Unit Tests
```bash
mvn test
```

### Run with Coverage
```bash
mvn clean test jacoco:report
```

Coverage report: `target/site/jacoco/index.html`

## 📊 Database Access

### Connect to PostgreSQL
```bash
docker exec -it cinema_postgres psql -U cinema_user -d cinema_db
```

### Useful Queries
```sql
-- Check all show seats
SELECT * FROM show_seats;

-- Check bookings
SELECT * FROM bookings;

-- See version changes (after concurrent booking test)
SELECT id, status, version FROM show_seats WHERE id IN (1, 2, 3);
```

## 🔍 Key Implementation Details

### Transaction Management
- `@Transactional` on `BookingService.bookSeats()`
- **All-or-nothing**: If booking 3 seats and 1 fails, all rollback

### Validation
- DTO validation with `@Valid`
- Custom business rules (show must be in future)
- Seat ownership verification (seats belong to show)

### Error Handling
- Global exception handler for consistent API responses
- Specific handlers for optimistic locking, validation, authentication

## 🚀 Deployment Considerations

### Configuration
Update `application.properties` for production:
```properties
# Use environment variables
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASS}

# Strong JWT secret
jwt.secret=${JWT_SECRET}

# Disable DDL auto
spring.jpa.hibernate.ddl-auto=validate

# Disable SQL logging
spring.jpa.show-sql=false
```

### Scaling
- Stateless design (JWT) allows horizontal scaling
- Database connection pooling (HikariCP)
- Consider Redis for caching show listings

## 📝 Project Status

✅ Phase 1: Skeleton - Complete
✅ Phase 2: Data Layer - Complete
✅ Phase 3: Read API - Complete
✅ Phase 4: Booking Logic - Complete
✅ Phase 5: Security & Testing - Complete

## 🎓 Learning Outcomes

This project demonstrates:
- ✅ Handling race conditions with optimistic locking
- ✅ Transaction management in Spring
- ✅ RESTful API design
- ✅ JWT authentication
- ✅ Database schema design
- ✅ Error handling best practices
- ✅ Clean architecture (entities, DTOs, services, controllers)

## 📄 License

This is a portfolio project for educational purposes.

## 👤 Author

Built following professional backend engineering practices for handling high-concurrency scenarios.

