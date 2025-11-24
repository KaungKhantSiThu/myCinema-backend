#!/bin/bash

# Quick Start Script for myCinema Project
# This script helps you get started quickly

set -e  # Exit on error

echo "╔════════════════════════════════════════╗"
echo "║  🎬 myCinema - Quick Start Script     ║"
echo "╚════════════════════════════════════════╝"
echo ""

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check prerequisites
echo "📋 Checking prerequisites..."
echo ""

if ! command_exists java; then
    echo "❌ Java is not installed. Please install Java 21."
    exit 1
fi

if ! command_exists mvn; then
    echo "❌ Maven is not installed. Please install Maven 3.8+."
    exit 1
fi

if ! command_exists docker; then
    echo "❌ Docker is not installed. Please install Docker."
    exit 1
fi

echo "✅ Java: $(java -version 2>&1 | head -1)"
echo "✅ Maven: $(mvn --version | head -1)"
echo "✅ Docker: $(docker --version)"
echo ""

# Start PostgreSQL
echo "🐘 Starting PostgreSQL..."
if docker ps | grep -q cinema_postgres; then
    echo "✅ PostgreSQL is already running"
else
    echo "   Starting PostgreSQL container..."
    docker-compose up -d
    echo "   Waiting for PostgreSQL to be ready..."
    sleep 5
    echo "✅ PostgreSQL started"
fi
echo ""

# Build the project
echo "🔨 Building the project..."
if mvn clean install -DskipTests > /tmp/mvn-build.log 2>&1; then
    echo "✅ Build successful!"
else
    echo "❌ Build failed. Check /tmp/mvn-build.log for details"
    tail -20 /tmp/mvn-build.log
    exit 1
fi
echo ""

echo "═══════════════════════════════════════"
echo "✨ Setup Complete! ✨"
echo "═══════════════════════════════════════"
echo ""
echo "🚀 To start the application, run:"
echo "   mvn spring-boot:run"
echo ""
echo "📡 The application will be available at:"
echo "   http://localhost:8080"
echo ""
echo "📚 Next steps:"
echo "   1. See README.md for API documentation"
echo "   2. Import postman-collection.json to test the API"
echo "   3. Run ./test-concurrency.sh to test optimistic locking"
echo ""
echo "🔍 Useful commands:"
echo "   - View logs: docker logs cinema_postgres"
echo "   - Stop database: docker-compose down"
echo "   - Connect to DB: docker exec -it cinema_postgres psql -U cinema_user -d cinema_db"
echo ""

