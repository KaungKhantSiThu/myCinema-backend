# myCinema Application Startup Guide

## Prerequisites

### 1. Java 21 LTS
**Already Installed**
- Location: `/Users/kaungkhantsithu/.jdk/jdk-21.0.8(1)/jdk-21.0.8+9/Contents/Home`
- Verify: `java -version` (should show Java 21)

### 2. PostgreSQL Database
**Required for Application Startup**

The application needs PostgreSQL 16 running on `localhost:5432`.

#### Option A: Using Docker Compose (Recommended)

1. **Start Docker Desktop**
   - Open Docker Desktop application
   - Wait for it to be fully running

2. **Start PostgreSQL Container**
   ```bash
   docker-compose up -d
   ```

3. **Verify PostgreSQL is Running**
   ```bash
   docker-compose ps
   ```
   
   You should see:
   ```
   NAME               IMAGE                 STATUS
   cinema_postgres    postgres:16-alpine    Up
   ```

4. **Check Database Health**
   ```bash
   docker exec -it cinema_postgres pg_isready -U cinema_user -d cinema_db
   ```

#### Option B: Install PostgreSQL Locally

1. **Install PostgreSQL 16**
   ```bash
   brew install postgresql@16
   ```

2. **Start PostgreSQL Service**
   ```bash
   brew services start postgresql@16
   ```

3. **Create Database and User**
   ```bash
   psql postgres
   ```
   
   Then run:
   ```sql
   CREATE USER cinema_user WITH PASSWORD 'cinema_pass';
   CREATE DATABASE cinema_db OWNER cinema_user;
   \q
   ```

## Starting the Application

### Step 1: Set Environment Variables

```bash
export TMDB_API_KEY="your_api_key_here"
```

### Step 2: Start the Application

```bash
./mvnw spring-boot:run
```

### Step 3: Verify Application Started

Wait for the log message:
```
Started MyCinemaApplication in X.XXX seconds
```

### Step 4: Access the Application

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Base**: http://localhost:8080/api
- **Health Check**: http://localhost:8080/actuator/health

## Testing the Application

### Run All Tests

```bash
./mvnw clean test
```

**Expected Result**: `Tests run: 59, Failures: 0, Errors: 0, Skipped: 0`

### Generate Test Coverage Report

```bash
./mvnw clean test jacoco:report
```

Coverage report will be at: `target/site/jacoco/index.html`

## Testing TMDb Integration

### 1. Get Admin Token

**Register Admin User** (via Swagger UI or curl):
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@cinema.com",
    "password": "AdminPass123!",
    "fullName": "Admin User",
    "role": "ADMIN"
  }'
```

**Login**:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@cinema.com",
    "password": "AdminPass123!"
  }'
```

Copy the `accessToken` from the response.

### 2. Search Movies from TMDb

```bash
curl -X GET "http://localhost:8080/api/admin/movies/search?query=Inception&page=1" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 3. Import a Movie

```bash
curl -X POST http://localhost:8080/api/admin/movies/import \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "externalId": "27205",
    "overrideGenres": []
  }'
```

### 4. Verify Movie Was Imported

```bash
curl -X GET http://localhost:8080/api/movies \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## Troubleshooting

### Issue: "Connection to localhost:5432 refused"

**Cause**: PostgreSQL is not running.

**Solution**:
- If using Docker: `docker-compose up -d`
- If using Homebrew: `brew services start postgresql@16`
- Verify: `nc -zv localhost 5432` (should show "succeeded")

### Issue: "Cannot connect to the Docker daemon"

**Cause**: Docker Desktop is not running.

**Solution**:
1. Open Docker Desktop application
2. Wait for it to fully start
3. Retry `docker-compose up -d`

### Issue: Tests Pass but Application Won't Start

**Cause**: Tests use H2 in-memory database, but the application requires PostgreSQL.

**Solution**: Follow the PostgreSQL setup steps above.

### Issue: TMDb API Returns 401 Unauthorized

**Cause**: Invalid or missing API key.

**Solution**:
1. Verify environment variable: `echo $TMDB_API_KEY`
2. Restart application after setting the variable
3. Check `application.properties` has `tmdb.api.enabled=true` (default)

### Issue: Admin Endpoints Return 403 Forbidden

**Cause**: User doesn't have ADMIN role.

**Solution**:
1. Register a new user with `"role": "ADMIN"`
2. Or manually update the database:
   ```sql
   UPDATE users SET role = 'ADMIN' WHERE email = 'your@email.com';
   ```

## Database Management

### Stop PostgreSQL Container

```bash
docker-compose down
```

### Stop and Remove Data

```bash
docker-compose down -v
```

### View PostgreSQL Logs

```bash
docker-compose logs -f postgres
```

### Connect to Database

```bash
docker exec -it cinema_postgres psql -U cinema_user -d cinema_db
```

### Reset Database (Re-run Migrations)

1. Stop application
2. Drop and recreate database:
   ```bash
   docker exec -it cinema_postgres psql -U cinema_user -d postgres -c "DROP DATABASE IF EXISTS cinema_db; CREATE DATABASE cinema_db OWNER cinema_user;"
   ```
3. Restart application (Flyway will run migrations automatically)

## Next Steps

1. [x] **PostgreSQL Running**: Verify with `docker-compose ps`
2. [x] **Application Started**: Check http://localhost:8080/actuator/health
3. [x] **TMDb Working**: Test movie search via Swagger UI
4. [ ] **Implement Unit Tests**: Task 6 in todo list
5. [ ] **Integration Tests**: Task 7 in todo list
6. [ ] **Production Readiness**: Task 8 in todo list

## Quick Start Script

Save this as `start.sh`:

```bash
#!/bin/bash
set -e

echo "Starting myCinema Application..."

# Check Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "Docker is not running. Please start Docker Desktop."
    exit 1
fi

# Start PostgreSQL
echo "Starting PostgreSQL..."
docker-compose up -d

# Wait for PostgreSQL to be ready
echo "Waiting for PostgreSQL to be ready..."
for i in {1..30}; do
    if docker exec cinema_postgres pg_isready -U cinema_user -d cinema_db > /dev/null 2>&1; then
        echo "PostgreSQL is ready!"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "PostgreSQL failed to start"
        exit 1
    fi
    sleep 1
done

# Set environment variables
export TMDB_API_KEY="your_api_key_here"

# Start application
echo "Starting application..."
./mvnw spring-boot:run
```

Make it executable:
```bash
chmod +x start.sh
./start.sh
```

## Support

For issues or questions:
- Check logs: Application logs show detailed error messages
- Review documentation: `TMDB_INTEGRATION_GUIDE.md`
- Database status: `docker-compose ps` and `docker-compose logs postgres`
