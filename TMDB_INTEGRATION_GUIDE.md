# 🎬 TMDb Integration Guide

This guide explains how to use The Movie Database (TMDb) integration in the Cinema Booking System to search and import movies.

## 📋 Table of Contents
- [Overview](#overview)
- [Setup](#setup)
- [API Endpoints](#api-endpoints)
- [Usage Workflow](#usage-workflow)
- [Swagger UI Guide](#swagger-ui-guide)
- [Troubleshooting](#troubleshooting)

## 🎯 Overview

The TMDb integration allows cinema administrators to:
- **Search** for movies in The Movie Database catalog
- **Import** selected movies into the cinema system
- **Create shows** for imported movies
- **Manage** movie catalog efficiently

### Architecture

The integration follows a clean, modular design:

```
Admin → AdminMovieController → MovieImportService → TmdbMovieDataSource → TmdbClient → TMDb API
                                                      (Adapter Pattern)
```

**Key Features:**
- ✅ Separate authentication (TMDb API key vs JWT tokens)
- ✅ Conditional loading (enable/disable via configuration)
- ✅ Proper error handling and logging
- ✅ Comprehensive Swagger documentation
- ✅ Admin-only access with role-based authorization

## 🔧 Setup

### 1. Get TMDb API Key

1. Go to [The Movie Database](https://www.themoviedb.org/)
2. Create an account (free)
3. Navigate to [API Settings](https://www.themoviedb.org/settings/api)
4. Request an API key (instant approval for personal use)
5. Copy your API key

### 2. Configure Application

**Option A: Environment Variable (Recommended for Production)**
```bash
export TMDB_API_KEY=your_api_key_here
export TMDB_API_ENABLED=true
```

**Option B: Application Properties (Development)**
```properties
# src/main/resources/application.properties
tmdb.api.key=your_api_key_here
tmdb.api.enabled=true
```

**Option C: Command Line**
```bash
./mvnw spring-boot:run -Dtmdb.api.key=your_api_key_here -Dtmdb.api.enabled=true
```

### 3. Verify Configuration

Check the startup logs for:
```
TMDb client initialized successfully. Base URL: https://api.themoviedb.org/3
TMDb Client initialized and ready for use
TMDb MovieDataSource initialized with custom client
```

## 🔌 API Endpoints

### Base URL
```
http://localhost:8080/api/admin/movies
```

### Authentication
All endpoints require:
- ✅ Valid JWT token in Authorization header
- ✅ ROLE_ADMIN role

### Endpoints

#### 1. 🔍 Search Movies
**Endpoint:** `GET /api/admin/movies/search`

**Parameters:**
| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| query | string | Yes | Search query (title, actor, keyword) | "Inception" |
| page | integer | No | Page number (default: 1) | 1 |

**Response:** Array of `ExternalMovieSearchResponse`
```json
[
  {
    "externalId": "27205",
    "title": "Inception",
    "overview": "A thief who steals corporate secrets...",
    "releaseDate": "2010-07-16",
    "runtime": 148,
    "genres": ["Action", "Science Fiction", "Thriller"],
    "posterPath": "/qmDpIHrmpJINaRKAfWQfftjCdyi.jpg",
    "voteAverage": 8.3,
    "source": "TMDb"
  }
]
```

**HTTP Status Codes:**
- `200 OK` - Search successful (may return empty array)
- `400 Bad Request` - Invalid parameters
- `401 Unauthorized` - Missing or invalid JWT token
- `403 Forbidden` - Not an admin user
- `502 Bad Gateway` - TMDb API unavailable or invalid API key

---

#### 2. 📥 Import Movie
**Endpoint:** `POST /api/admin/movies/import`

**Request Body:** `ImportMovieRequest`
```json
{
  "externalId": "27205",
  "genre": "Action"
}
```

**Fields:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| externalId | string | Yes | TMDb movie ID from search results |
| genre | string | No | Override genre (uses TMDb's first genre if omitted) |

**Response:** `MovieResponse`
```json
{
  "id": 1,
  "title": "Inception",
  "durationMinutes": 148,
  "genre": "Action"
}
```

**HTTP Status Codes:**
- `201 Created` - Movie imported successfully
- `400 Bad Request` - Invalid request body
- `401 Unauthorized` - Missing or invalid JWT token
- `403 Forbidden` - Not an admin user
- `404 Not Found` - Movie not found in TMDb with provided ID
- `502 Bad Gateway` - TMDb API unavailable

## 📖 Usage Workflow

### Step-by-Step Guide

#### 1. **Login as Admin**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@cinema.com",
    "password": "Admin123!"
  }'
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "id": 1,
  "email": "admin@cinema.com",
  "roles": "ROLE_ADMIN"
}
```

**Save the token** for subsequent requests.

---

#### 2. **Search for Movies**
```bash
curl -X GET "http://localhost:8080/api/admin/movies/search?query=Inception&page=1" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Response:**
```json
[
  {
    "externalId": "27205",
    "title": "Inception",
    "overview": "Cobb, a skilled thief...",
    "releaseDate": "2010-07-16",
    "runtime": 148,
    "genres": ["Action", "Science Fiction", "Thriller"],
    "posterPath": "/qmDpIHrmpJINaRKAfWQfftjCdyi.jpg",
    "voteAverage": 8.3,
    "source": "TMDb"
  }
]
```

---

#### 3. **Import Selected Movie**
```bash
curl -X POST http://localhost:8080/api/admin/movies/import \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "externalId": "27205",
    "genre": "Science Fiction"
  }'
```

**Response:**
```json
{
  "id": 1,
  "title": "Inception",
  "durationMinutes": 148,
  "genre": "Science Fiction"
}
```

---

#### 4. **Create a Show**
Now that the movie is in your system, create a show:

```bash
curl -X POST http://localhost:8080/api/admin/shows \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "movieId": 1,
    "hallId": 1,
    "startTime": "2025-12-15T19:00:00",
    "ticketPrice": 15.00
  }'
```

---

#### 5. **Verify in Public API**
The movie is now available to users:

```bash
curl -X GET http://localhost:8080/api/movies
```

## 🎨 Swagger UI Guide

### Accessing Swagger UI

1. Open your browser to: **http://localhost:8080/swagger-ui.html**
2. You'll see all API endpoints organized by tags

### Using TMDb Endpoints in Swagger

#### 1. **Authorize**
- Click the **"Authorize" button** (🔒 icon) at the top right
- Enter: `Bearer YOUR_JWT_TOKEN` (include the word "Bearer" and a space)
- Click "Authorize" then "Close"

#### 2. **Search Movies**
- Expand **"🎥 Admin - TMDb Movie Import"** section
- Click **"GET /api/admin/movies/search"**
- Click **"Try it out"**
- Enter search query (e.g., "Inception")
- Click **"Execute"**
- View response below

#### 3. **Import Movie**
- Still in **"🎥 Admin - TMDb Movie Import"** section
- Click **"POST /api/admin/movies/import"**
- Click **"Try it out"**
- Copy an `externalId` from search results
- Paste into the request body:
  ```json
  {
    "externalId": "27205",
    "genre": "Action"
  }
  ```
- Click **"Execute"**
- Movie is now imported!

### Swagger Features

✨ **Auto-completion**: Swagger provides examples and schema validation

✨ **Response Samples**: See expected responses for each status code

✨ **Request Examples**: Copy-paste ready JSON examples

✨ **Error Documentation**: All possible error codes documented

✨ **Try It Out**: Test APIs directly from the browser

## 🚨 Troubleshooting

### Issue: Endpoints Not Showing in Swagger

**Solution:**
- Verify TMDb is enabled: `tmdb.api.enabled=true`
- Check logs for "TMDb client initialized"
- Restart application after configuration changes

### Issue: 401 Unauthorized

**Solution:**
- Ensure you're logged in as admin
- Check JWT token is valid and not expired
- Format: `Bearer <token>` (note the space)
- Admin account: `admin@cinema.com` / `Admin123!`

### Issue: 403 Forbidden

**Solution:**
- Verify user has ROLE_ADMIN role
- Check: `GET /api/auth/me` with your token
- Default admin account has ROLE_ADMIN

### Issue: 502 Bad Gateway (TMDb API Error)

**Possible Causes:**
1. **Invalid API Key**
   - Verify key is correct
   - Check for extra spaces or quotes
   - Get new key from TMDb if needed

2. **Rate Limiting**
   - TMDb free tier: 40 requests / 10 seconds
   - Wait a moment and try again
   - Consider caching responses

3. **TMDb Service Down**
   - Check [TMDb Status](https://status.themoviedb.org/)
   - Try again later

### Issue: 404 Movie Not Found

**Solution:**
- Verify the externalId is correct
- TMDb IDs are numeric strings (e.g., "27205")
- Movie might have been removed from TMDb
- Try searching again to get current ID

### Issue: Empty Search Results

**Possible Causes:**
- Search query too specific
- Try broader terms (e.g., "Inception" instead of "Inception 2010 DiCaprio")
- Check spelling
- Try searching by actor name

### Checking Logs

View detailed logs:
```bash
tail -f logs/application.log | grep TMDb
```

Common log patterns:
- `TMDb client initialized` - Configuration successful
- `TMDb search successful` - Search completed
- `Movie imported successfully` - Import completed
- `TMDb API error` - API communication issue

## 📚 Additional Resources

- [TMDb API Documentation](https://developers.themoviedb.org/3)
- [Project README](./README.md)
- [API Documentation](./API_DOCUMENTATION.md)
- [Deployment Guide](./DEPLOYMENT.md)

## 🎯 Best Practices

1. **API Key Security**
   - Never commit API keys to version control
   - Use environment variables in production
   - Rotate keys periodically

2. **Rate Limiting**
   - Cache frequently accessed movies
   - Implement request throttling
   - Monitor API usage

3. **Error Handling**
   - Always check response status codes
   - Implement retry logic for transient failures
   - Log errors for debugging

4. **Data Management**
   - Don't import duplicate movies
   - Verify movie data before creating shows
   - Keep movie information up to date

## 🤝 Support

If you encounter issues:
1. Check this guide's troubleshooting section
2. Review application logs
3. Verify TMDb API status
4. Check your JWT token and admin role

---

**Happy Movie Importing! 🎬🍿**
