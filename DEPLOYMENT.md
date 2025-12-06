# 🚀 Cinema Booking Service - Deployment Guide

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Local Development Setup](#local-development-setup)
3. [Production Deployment](#production-deployment)
4. [Environment Variables](#environment-variables)
5. [Database Setup](#database-setup)
6. [Docker Deployment](#docker-deployment)
7. [Kubernetes Deployment](#kubernetes-deployment)
8. [Monitoring & Health Checks](#monitoring--health-checks)
9. [Troubleshooting](#troubleshooting)

---

## Prerequisites

- **Java 21** or higher
- **Maven 3.8+**
- **PostgreSQL 16+**
- **Docker** (optional, for containerized deployment)
- **Kubernetes** (optional, for K8s deployment)

---

## Local Development Setup

### 1. Clone the Repository
```bash
git clone <repository-url>
cd myCinema
```

### 2. Set Up Environment Variables
```bash
# Copy the example file
cp .env.example .env

# Edit .env with your actual values
nano .env
```

Required environment variables:
```bash
DATABASE_URL=jdbc:postgresql://localhost:5432/cinema_db
DATABASE_USERNAME=cinema_user
DATABASE_PASSWORD=your_secure_password
JWT_SECRET=$(openssl rand -base64 32)
JWT_EXPIRATION=86400000
SERVER_PORT=8080
```

### 3. Start PostgreSQL with Docker
```bash
docker-compose up -d postgres
```

Or install PostgreSQL locally and create the database:
```sql
CREATE DATABASE cinema_db;
CREATE USER cinema_user WITH ENCRYPTED PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE cinema_db TO cinema_user;
```

### 4. Run the Application
```bash
# Using Maven
./mvnw spring-boot:run

# Or build and run JAR
./mvnw clean package
java -jar target/myCinema-0.0.1-SNAPSHOT.jar
```

### 5. Verify the Application
```bash
# Health check
curl http://localhost:8080/actuator/health

# API documentation
open http://localhost:8080/swagger-ui.html
```

---

## Production Deployment

### 1. Generate Secure Secrets
```bash
# Generate JWT secret (32 bytes, base64 encoded)
openssl rand -base64 32

# Generate database password (32 chars, alphanumeric + special chars)
openssl rand -base64 32 | tr -d "=+/" | cut -c1-32
```

### 2. Set Environment Variables

**For Linux/macOS (systemd):**
Create `/etc/systemd/system/cinema-booking.service`:
```ini
[Unit]
Description=Cinema Booking Service
After=network.target postgresql.service

[Service]
Type=simple
User=cinema
WorkingDirectory=/opt/cinema-booking
ExecStart=/usr/bin/java -jar -Dspring.profiles.active=prod /opt/cinema-booking/myCinema.jar
Environment="DATABASE_URL=jdbc:postgresql://localhost:5432/cinema_db"
Environment="DATABASE_USERNAME=cinema_user"
Environment="DATABASE_PASSWORD=REPLACE_WITH_SECURE_PASSWORD"
Environment="JWT_SECRET=REPLACE_WITH_GENERATED_SECRET"
Environment="JWT_EXPIRATION=86400000"
Environment="SERVER_PORT=8080"
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Enable and start:
```bash
sudo systemctl daemon-reload
sudo systemctl enable cinema-booking
sudo systemctl start cinema-booking
sudo systemctl status cinema-booking
```

**For Docker:**
Create `.env` file (never commit this):
```bash
DATABASE_URL=jdbc:postgresql://postgres:5432/cinema_db
DATABASE_USERNAME=cinema_user
DATABASE_PASSWORD=your_secure_password
JWT_SECRET=your_generated_secret
JWT_EXPIRATION=86400000
```

### 3. Database Migration
Flyway will automatically run migrations on startup. To manually run:
```bash
./mvnw flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/cinema_db \
  -Dflyway.user=cinema_user \
  -Dflyway.password=your_password
```

### 4. Build Production JAR
```bash
# Clean build with tests
./mvnw clean package

# Build without tests (faster)
./mvnw clean package -DskipTests

# The JAR will be in: target/myCinema-0.0.1-SNAPSHOT.jar
```

---

## Environment Variables

### Required Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `DATABASE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/cinema_db` |
| `DATABASE_USERNAME` | Database username | `cinema_user` |
| `DATABASE_PASSWORD` | Database password | `your_secure_password` |
| `JWT_SECRET` | JWT signing secret (min 256 bits) | Generated with `openssl rand -base64 32` |
| `JWT_EXPIRATION` | Token expiration in ms | `86400000` (24 hours) |

### Optional Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | HTTP port | `8080` |
| `LOGGING_LEVEL` | Log level | `INFO` |
| `SPRING_PROFILES_ACTIVE` | Active profile | `default` |

---

## Docker Deployment

### 1. Build Docker Image
```bash
docker build -t cinema-booking:latest .
```

### 2. Run with Docker Compose
```bash
# Start all services (app + postgres)
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop all services
docker-compose down
```

### 3. Run Standalone Container
```bash
docker run -d \
  --name cinema-booking \
  -p 8080:8080 \
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/cinema_db \
  -e DATABASE_USERNAME=cinema_user \
  -e DATABASE_PASSWORD=your_password \
  -e JWT_SECRET=your_secret \
  cinema-booking:latest
```

### 4. Health Check
```bash
docker ps
curl http://localhost:8080/actuator/health
```

---

## Kubernetes Deployment

### 1. Create ConfigMap
```bash
kubectl create configmap cinema-config \
  --from-literal=DATABASE_URL=jdbc:postgresql://postgres-service:5432/cinema_db \
  --from-literal=SERVER_PORT=8080
```

### 2. Create Secret
```bash
kubectl create secret generic cinema-secrets \
  --from-literal=DATABASE_USERNAME=cinema_user \
  --from-literal=DATABASE_PASSWORD=your_secure_password \
  --from-literal=JWT_SECRET=your_jwt_secret
```

### 3. Deploy
```bash
# Apply all K8s manifests
kubectl apply -f k8s/

# Check deployment status
kubectl get pods -l app=cinema-booking
kubectl get svc cinema-booking-service

# View logs
kubectl logs -f deployment/cinema-booking
```

### 4. Scale
```bash
# Scale to 3 replicas
kubectl scale deployment cinema-booking --replicas=3

# Auto-scaling
kubectl autoscale deployment cinema-booking --min=2 --max=10 --cpu-percent=80
```

---

## Monitoring & Health Checks

### Health Endpoints
```bash
# Liveness probe
curl http://localhost:8080/actuator/health/liveness

# Readiness probe
curl http://localhost:8080/actuator/health/readiness

# Full health details (requires authentication)
curl http://localhost:8080/actuator/health
```

### Metrics
```bash
# Prometheus metrics
curl http://localhost:8080/actuator/prometheus

# Application metrics
curl http://localhost:8080/actuator/metrics
```

### Logs
```bash
# Docker logs
docker logs -f cinema-booking

# Systemd logs
journalctl -u cinema-booking -f

# K8s logs
kubectl logs -f deployment/cinema-booking
```

---

## Troubleshooting

### Application Won't Start

**1. Check database connection:**
```bash
psql -h localhost -U cinema_user -d cinema_db
```

**2. Verify environment variables:**
```bash
# Docker
docker exec cinema-booking env | grep DATABASE

# Systemd
systemctl show cinema-booking --property=Environment
```

**3. Check logs:**
```bash
# Docker
docker logs cinema-booking

# Systemd
journalctl -u cinema-booking -n 100
```

### Database Migration Fails

**1. Check Flyway status:**
```sql
SELECT * FROM flyway_schema_history;
```

**2. Manually repair:**
```bash
./mvnw flyway:repair
```

**3. Reset (CAUTION - deletes all data):**
```sql
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
```

### High Memory Usage

**1. Check JVM settings:**
```bash
# Add to java command
-XX:MaxRAMPercentage=75.0
-XX:+UseContainerSupport
-Xms512m
-Xmx2g
```

**2. Monitor with JVM tools:**
```bash
jcmd <pid> GC.heap_info
jcmd <pid> VM.native_memory summary
```

### Connection Pool Exhausted

**1. Check active connections:**
```sql
SELECT count(*) FROM pg_stat_activity WHERE datname = 'cinema_db';
```

**2. Increase pool size in application.properties:**
```properties
spring.datasource.hikari.maximum-pool-size=100
```

### Authentication Issues

**1. Verify JWT secret is set:**
```bash
echo $JWT_SECRET
```

**2. Test login endpoint:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}'
```

**3. Decode JWT token:**
```bash
# Use jwt.io or:
echo "YOUR_TOKEN" | cut -d'.' -f2 | base64 -d | jq
```

---

## Security Checklist

Before going to production:

- [ ] All secrets in environment variables (not in code)
- [ ] JWT secret is strong (min 256 bits)
- [ ] Database password is strong
- [ ] HTTPS enabled (use reverse proxy like Nginx)
- [ ] Rate limiting configured
- [ ] CORS configured properly
- [ ] Security headers enabled
- [ ] Logging doesn't expose sensitive data
- [ ] Actuator endpoints secured
- [ ] Database backups configured
- [ ] Monitoring alerts set up

---

## Performance Tuning

### Database Optimization
```sql
-- Add indexes for frequently queried columns
CREATE INDEX idx_bookings_user_id ON bookings(user_id);
CREATE INDEX idx_show_seats_show_id ON show_seats(show_id);

-- Analyze query performance
EXPLAIN ANALYZE SELECT * FROM bookings WHERE user_id = 1;
```

### Application Tuning
```properties
# Increase connection pool
spring.datasource.hikari.maximum-pool-size=50

# Enable HTTP/2
server.http2.enabled=true

# Increase compression
server.compression.min-response-size=512
```

### JVM Tuning
```bash
java -jar \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+UseStringDeduplication \
  -XX:+ParallelRefProcEnabled \
  myCinema.jar
```

---

## Support

For issues and questions:
- Check logs first
- Review [RECOMMENDATIONS.md](RECOMMENDATIONS.md)
- Open an issue on GitHub
- Contact: your-email@example.com

---

**Last Updated:** November 30, 2025

