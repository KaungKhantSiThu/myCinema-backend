#!/bin/bash
set -e

echo "Starting myCinema Application..."

# Check Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "Docker is not running. Please start Docker Desktop."
    echo "   Open Docker Desktop and wait for it to be fully running."
    exit 1
fi

# Start PostgreSQL
echo "Starting PostgreSQL..."
# If a container named `cinema_postgres` already exists, start/reuse it instead of recreating
existing_container_id=$(docker ps -a --filter "name=cinema_postgres" -q)
if [ -n "$existing_container_id" ]; then
    # If it's running, reuse it
    if docker ps --filter "id=$existing_container_id" --filter "status=running" -q >/dev/null 2>&1; then
        echo "PostgreSQL container already running. Reusing existing container."
    else
        echo "Starting existing PostgreSQL container..."
        docker start "$existing_container_id"
    fi
else
    docker-compose up -d
fi

# Wait for PostgreSQL to be ready
echo "Waiting for PostgreSQL to be ready..."
for i in {1..30}; do
    if docker exec cinema_postgres pg_isready -U cinema_user -d cinema_db > /dev/null 2>&1; then
        echo "PostgreSQL is ready!"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "PostgreSQL failed to start"
        echo "   Check logs with: docker-compose logs postgres"
        exit 1
    fi
    sleep 1
done

# Set environment variables
# Load environment variables from .env if it exists
if [ -f .env ]; then
    echo "Loading environment variables from .env file..."
    export $(cat .env | xargs)
fi

# Check for TMDb API Key
if [ -z "$TMDB_API_KEY" ]; then
    echo "WARNING: TMDB_API_KEY is not set. Movie import feature may not work."
    echo "    Create a .env file based on .env.example or export the variable manually."
else 
    echo "TMDb API Key found in environment."
fi

# Start application
echo "Starting application..."
echo "   Access Swagger UI at: http://localhost:8080/swagger-ui.html"
echo "   Press Ctrl+C to stop the application"
echo ""
./mvnw spring-boot:run
