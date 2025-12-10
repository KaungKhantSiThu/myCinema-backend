package com.kkst.mycinema.tmdbclient.config;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for TMDb API client.
 * Binds to properties with prefix "tmdb".
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "tmdb")
@ConditionalOnProperty(name = "tmdb.api.enabled", havingValue = "true")
public class TmdbClientConfig {

    /**
     * TMDb API configuration
     */
    private Api api = new Api();

    /**
     * HTTP client configuration
     */
    private Client client = new Client();

    /**
     * Retry configuration
     */
    private Retry retry = new Retry();

    @Data
    public static class Api {
        /**
         * TMDb API key (required)
         */
        private String key;

        /**
         * Whether TMDb API integration is enabled
         */
        private boolean enabled = true;

        /**
         * TMDb API base URL
         */
        private String baseUrl = "https://api.themoviedb.org/3";

        /**
         * TMDb API version
         */
        private String version = "3";

        /**
         * Default language for API requests
         */
        private String language = "en-US";

        /**
         * Include adult content in search results
         */
        private boolean includeAdult = false;
    }

    @Data
    public static class Client {
        /**
         * Connection timeout in milliseconds
         */
        private int connectTimeout = 5000;

        /**
         * Read timeout in milliseconds
         */
        private int readTimeout = 10000;

        /**
         * Maximum number of connections
         */
        private int maxConnections = 50;
    }

    @Data
    public static class Retry {
        /**
         * Maximum number of retry attempts
         */
        private int maxAttempts = 3;

        /**
         * Initial backoff delay in milliseconds
         */
        private long backoffDelay = 1000;

        /**
         * Backoff multiplier
         */
        private double backoffMultiplier = 2.0;
    }
}

