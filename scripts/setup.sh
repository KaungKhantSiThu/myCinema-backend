#!/bin/bash

# Cinema Booking Service - Quick Setup Script
# This script helps you set up the environment for local development

set -e

echo "🎬 Cinema Booking Service - Quick Setup"
echo "========================================"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if .env already exists
if [ -f .env ]; then
    echo -e "${YELLOW}⚠️  .env file already exists!${NC}"
    read -p "Do you want to overwrite it? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Keeping existing .env file."
        exit 0
    fi
fi

echo "Creating .env file from template..."
cp .env.example .env

# Generate JWT secret
echo ""
echo "Generating secure JWT secret..."
JWT_SECRET=$(openssl rand -base64 32)
echo -e "${GREEN}✓ JWT secret generated${NC}"

# Generate database password
echo "Generating database password..."
DB_PASSWORD=$(openssl rand -base64 24 | tr -d "=+/" | cut -c1-20)
echo -e "${GREEN}✓ Database password generated${NC}"

# Update .env file
echo ""
echo "Updating .env file with generated secrets..."

# macOS compatible sed command
if [[ "$OSTYPE" == "darwin"* ]]; then
    sed -i '' "s|DATABASE_PASSWORD=.*|DATABASE_PASSWORD=${DB_PASSWORD}|" .env
    sed -i '' "s|JWT_SECRET=.*|JWT_SECRET=${JWT_SECRET}|" .env
else
    sed -i "s|DATABASE_PASSWORD=.*|DATABASE_PASSWORD=${DB_PASSWORD}|" .env
    sed -i "s|JWT_SECRET=.*|JWT_SECRET=${JWT_SECRET}|" .env
fi

echo -e "${GREEN}✓ .env file created successfully${NC}"
echo ""

# Display the configuration
echo "📝 Your configuration:"
echo "===================="
cat .env
echo ""

# Ask if user wants to start PostgreSQL
echo ""
read -p "🐘 Do you want to start PostgreSQL with Docker? (Y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Nn]$ ]]; then
    echo "Starting PostgreSQL..."
    docker-compose up -d postgres
    echo -e "${GREEN}✓ PostgreSQL started${NC}"
    echo "Waiting for PostgreSQL to be ready..."
    sleep 3
fi

# Ask if user wants to run the application
echo ""
read -p "🚀 Do you want to start the application now? (Y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Nn]$ ]]; then
    echo "Starting application..."
    export $(cat .env | xargs)
    ./mvnw spring-boot:run
else
    echo ""
    echo "Setup complete! To start the application, run:"
    echo ""
    echo -e "${YELLOW}export \$(cat .env | xargs)${NC}"
    echo -e "${YELLOW}./mvnw spring-boot:run${NC}"
    echo ""
fi

echo ""
echo -e "${GREEN}✓ Setup complete!${NC}"
echo ""
echo "📚 Next steps:"
echo "  1. Visit http://localhost:8080/swagger-ui.html for API docs"
echo "  2. Health check: curl http://localhost:8080/actuator/health"
echo "  3. Read DEPLOYMENT.md for more details"
echo ""

