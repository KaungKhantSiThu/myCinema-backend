#!/bin/bash

# TMDb Integration Verification Script
# Verifies that the custom TMDb client is properly integrated

echo "=================================="
echo "TMDb Integration Verification"
echo "=================================="
echo ""

# Check if TmdbMovieDataSource uses custom client
echo "1. Checking TmdbMovieDataSource implementation..."
if grep -q "TmdbClient tmdbClient" src/main/java/com/kkst/mycinema/external/tmdb/TmdbMovieDataSource.java && \
   grep -q "TmdbMovieMapper mapper" src/main/java/com/kkst/mycinema/external/tmdb/TmdbMovieDataSource.java; then
    echo "   ✅ TmdbMovieDataSource uses custom client"
else
    echo "   ❌ TmdbMovieDataSource does not use custom client"
    exit 1
fi

# Check for old library references
echo "2. Checking for old library references..."
OLD_REFS=$(find src -name "*.java" -type f -exec grep -l "info.movito.themoviedbapi" {} \; 2>/dev/null | wc -l)
if [ "$OLD_REFS" -eq 0 ]; then
    echo "   ✅ No references to old themoviedbapi library"
else
    echo "   ❌ Found $OLD_REFS file(s) with old library references"
    exit 1
fi

# Check if custom client exists
echo "3. Checking custom TMDb client files..."
REQUIRED_FILES=(
    "src/main/java/com/kkst/mycinema/tmdbclient/TmdbClient.java"
    "src/main/java/com/kkst/mycinema/tmdbclient/http/TmdbHttpClient.java"
    "src/main/java/com/kkst/mycinema/tmdbclient/service/TmdbMovieService.java"
    "src/main/java/com/kkst/mycinema/tmdbclient/mapper/TmdbMovieMapper.java"
    "src/main/java/com/kkst/mycinema/tmdbclient/config/TmdbClientConfig.java"
    "src/main/java/com/kkst/mycinema/tmdbclient/config/TmdbClientAutoConfiguration.java"
)

ALL_EXIST=true
for file in "${REQUIRED_FILES[@]}"; do
    if [ -f "$file" ]; then
        echo "   ✅ $file exists"
    else
        echo "   ❌ $file missing"
        ALL_EXIST=false
    fi
done

if [ "$ALL_EXIST" = false ]; then
    exit 1
fi

# Compile check
echo "4. Compiling TmdbMovieDataSource..."
./mvnw compile -DskipTests -q > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "   ✅ Compilation successful"
else
    echo "   ❌ Compilation failed"
    exit 1
fi

# Check if compiled class exists
if [ -f "target/classes/com/kkst/mycinema/external/tmdb/TmdbMovieDataSource.class" ]; then
    echo "   ✅ TmdbMovieDataSource.class exists"
else
    echo "   ❌ TmdbMovieDataSource.class not found"
    exit 1
fi

echo ""
echo "=================================="
echo "✅ ALL CHECKS PASSED"
echo "=================================="
echo ""
echo "Summary:"
echo "- TmdbMovieDataSource successfully migrated to custom client"
echo "- No references to old themoviedbapi library found"
echo "- All required custom client files present"
echo "- Compilation successful"
echo ""
echo "The Movie Datasource has been successfully replaced with the custom TMDb API client!"

