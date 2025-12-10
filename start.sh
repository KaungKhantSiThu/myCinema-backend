#!/bin/bash
set -e

echo "🚀 Starting myCinema Application..."

# Check Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker Desktop."
    echo "   Open Docker Desktop and wait for it to be fully running."
    exit 1
fi

# Start PostgreSQL
echo "📦 Starting PostgreSQL..."
docker-compose up -d

# Wait for PostgreSQL to be ready
echo "⏳ Waiting for PostgreSQL to be ready..."
for i in {1..30}; do
    if docker exec cinema_postgres pg_isready -U cinema_user -d cinema_db > /dev/null 2>&1; then
        echo "✅ PostgreSQL is ready!"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "❌ PostgreSQL failed to start"
        echo "   Check logs with: docker-compose logs postgres"
        exit 1
    fi
    sleep 1
done

# Set environment variables
# TMDb uses Bearer token for API v4, but the Java library uses the Read Access Token (Bearer token)
export TMDB_API_KEY="0b8723760cac397ab78965e78c1cd188"
echo "🔑 TMDb API Key configured"

# Start application
echo "🎬 Starting application..."
echo "   Access Swagger UI at: http://localhost:8080/swagger-ui.html"
echo "   Press Ctrl+C to stop the application"
echo ""
./mvnw spring-boot:run
