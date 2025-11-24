#!/bin/bash

# Test Script for Concurrent Booking
# This script simulates two users trying to book the same seats simultaneously

echo "🎬 Cinema Booking Concurrency Test"
echo "=================================="
echo ""

# Configuration
API_URL="http://localhost:8080"
EMAIL1="user1@test.com"
EMAIL2="user2@test.com"
PASSWORD="password123"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "📝 Step 1: Registering User 1..."
TOKEN1=$(curl -s -X POST "$API_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL1\",\"password\":\"$PASSWORD\"}" | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN1" ]; then
  echo "  User 1 might already exist, trying login..."
  TOKEN1=$(curl -s -X POST "$API_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$EMAIL1\",\"password\":\"$PASSWORD\"}" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
fi

echo -e "${GREEN}  ✓ User 1 authenticated${NC}"

echo ""
echo "📝 Step 2: Registering User 2..."
TOKEN2=$(curl -s -X POST "$API_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL2\",\"password\":\"$PASSWORD\"}" | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN2" ]; then
  echo "  User 2 might already exist, trying login..."
  TOKEN2=$(curl -s -X POST "$API_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$EMAIL2\",\"password\":\"$PASSWORD\"}" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
fi

echo -e "${GREEN}  ✓ User 2 authenticated${NC}"

echo ""
echo "📝 Step 3: Getting available shows..."
SHOW_ID=$(curl -s "$API_URL/api/shows" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
echo -e "${GREEN}  ✓ Found show ID: $SHOW_ID${NC}"

echo ""
echo "📝 Step 4: Getting available seats..."
SEATS=$(curl -s "$API_URL/api/shows/$SHOW_ID/seats" | grep -o '"seatId":[0-9]*' | cut -d':' -f2 | head -3 | tr '\n' ',' | sed 's/,$//')
echo -e "${GREEN}  ✓ Selected seats: $SEATS${NC}"

echo ""
echo "⚔️  Step 5: CONCURRENT BOOKING TEST"
echo "  Both users will try to book the same seats simultaneously..."
echo ""

# Create booking JSON
BOOKING_JSON="{\"showId\":$SHOW_ID,\"seatIds\":[$SEATS]}"

# Concurrent booking
echo "  🏃 User 1 booking..."
RESPONSE1=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "$API_URL/api/bookings" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN1" \
  -d "$BOOKING_JSON") &
PID1=$!

echo "  🏃 User 2 booking..."
RESPONSE2=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "$API_URL/api/bookings" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN2" \
  -d "$BOOKING_JSON") &
PID2=$!

# Wait for both requests
wait $PID1
wait $PID2

echo ""
echo "=================================="
echo "📊 RESULTS"
echo "=================================="

STATUS1=$(echo "$RESPONSE1" | grep HTTP_STATUS | cut -d':' -f2)
STATUS2=$(echo "$RESPONSE2" | grep HTTP_STATUS | cut -d':' -f2)

echo ""
echo "User 1 Response:"
if [ "$STATUS1" = "201" ]; then
  echo -e "${GREEN}  ✓ SUCCESS (HTTP 201)${NC}"
  echo "  Booking confirmed!"
elif [ "$STATUS1" = "409" ]; then
  echo -e "${YELLOW}  ⚠ CONFLICT (HTTP 409)${NC}"
  echo "  Seats already booked by another user."
else
  echo -e "${RED}  ✗ FAILED (HTTP $STATUS1)${NC}"
fi

echo ""
echo "User 2 Response:"
if [ "$STATUS2" = "201" ]; then
  echo -e "${GREEN}  ✓ SUCCESS (HTTP 201)${NC}"
  echo "  Booking confirmed!"
elif [ "$STATUS2" = "409" ]; then
  echo -e "${YELLOW}  ⚠ CONFLICT (HTTP 409)${NC}"
  echo "  Seats already booked by another user."
else
  echo -e "${RED}  ✗ FAILED (HTTP $STATUS2)${NC}"
fi

echo ""
echo "=================================="
echo "🎯 ANALYSIS"
echo "=================================="

if [ "$STATUS1" = "201" ] && [ "$STATUS2" = "409" ]; then
  echo -e "${GREEN}✓ OPTIMISTIC LOCKING WORKS!${NC}"
  echo "  User 1 succeeded, User 2 was correctly rejected."
elif [ "$STATUS2" = "201" ] && [ "$STATUS1" = "409" ]; then
  echo -e "${GREEN}✓ OPTIMISTIC LOCKING WORKS!${NC}"
  echo "  User 2 succeeded, User 1 was correctly rejected."
elif [ "$STATUS1" = "201" ] && [ "$STATUS2" = "201" ]; then
  echo -e "${RED}✗ DOUBLE BOOKING DETECTED!${NC}"
  echo "  Both users succeeded - THIS IS A BUG!"
else
  echo -e "${YELLOW}⚠ UNEXPECTED RESULT${NC}"
  echo "  Check application logs for details."
fi

echo ""

