#!/bin/bash

echo "=========================================="
echo "TMDb Client Integration Verification"
echo "=========================================="
echo

# Check if source files exist
echo "✓ Checking TMDb client files..."
if [ -f "src/main/java/com/kkst/mycinema/tmdbclient/TmdbClient.java" ]; then
    echo "  ✓ TmdbClient.java exists"
else
    echo "  ✗ TmdbClient.java missing"
    exit 1
fi

if [ -f "src/main/java/com/kkst/mycinema/tmdbclient/service/TmdbMovieService.java" ]; then
    echo "  ✓ TmdbMovieService.java exists"
else
    echo "  ✗ TmdbMovieService.java missing"
    exit 1
fi

if [ -f "src/main/java/com/kkst/mycinema/external/tmdb/TmdbMovieDataSource.java" ]; then
    echo "  ✓ TmdbMovieDataSource.java exists (integrated)"
else
    echo "  ✗ TmdbMovieDataSource.java missing"
    exit 1
fi

echo

# Check test files
echo "✓ Checking test files..."
if [ -f "src/test/java/com/kkst/mycinema/tmdbclient/TmdbClientIntegrationTest.java" ]; then
    echo "  ✓ TmdbClientIntegrationTest.java exists"
else
    echo "  ✗ TmdbClientIntegrationTest.java missing"
fi

if [ -f "src/test/java/com/kkst/mycinema/tmdbclient/mapper/TmdbMovieMapperTest.java" ]; then
    echo "  ✓ TmdbMovieMapperTest.java exists"
else
    echo "  ✗ TmdbMovieMapperTest.java missing"
fi

echo

# Count TMDb client files
echo "✓ TMDb client package statistics..."
CLIENT_FILES=$(find src/main/java/com/kkst/mycinema/tmdbclient -name "*.java" 2>/dev/null | wc -l)
echo "  - Java files: $CLIENT_FILES"

TEST_FILES=$(find src/test/java/com/kkst/mycinema/tmdbclient -name "*.java" 2>/dev/null | wc -l)
echo "  - Test files: $TEST_FILES"

echo

# Check documentation
echo "✓ Checking documentation..."
if [ -f "TMDB_CLIENT_README.md" ]; then
    echo "  ✓ TMDB_CLIENT_README.md exists"
else
    echo "  ✗ TMDB_CLIENT_README.md missing"
fi

echo

# Compile check
echo "✓ Compiling project..."
./mvnw compile -q -DskipTests > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "  ✓ Project compiles successfully"
else
    echo "  ✗ Compilation failed"
    exit 1
fi

echo

# Run mapper tests (unit tests, no dependencies)
echo "✓ Running TMDb mapper unit tests..."
./mvnw test -Dtest=TmdbMovieMapperTest -q > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "  ✓ All mapper tests passed"
else
    echo "  ✗ Some mapper tests failed"
    echo "  Run: ./mvnw test -Dtest=TmdbMovieMapperTest"
fi

echo
echo "=========================================="
echo "Verification Complete!"
echo "=========================================="
echo
echo "Next steps:"
echo "1. Set TMDb API key: export TMDB_API_KEY='your_key'"
echo "2. Enable TMDb: export TMDB_API_ENABLED='true'"
echo "3. Run integration tests: ./mvnw test -Dtest=TmdbClientIntegrationTest"
echo "4. Start the application: ./mvnw spring-boot:run"
echo

