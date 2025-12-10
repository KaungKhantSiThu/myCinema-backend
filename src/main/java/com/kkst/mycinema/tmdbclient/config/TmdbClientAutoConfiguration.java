package com.kkst.mycinema.tmdbclient.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Auto-configuration for TMDb client.
 * Sets up RestTemplate and other necessary beans.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "tmdb.api.enabled", havingValue = "true")
public class TmdbClientAutoConfiguration {

    private final TmdbClientConfig config;

    public TmdbClientAutoConfiguration(TmdbClientConfig config) {
        this.config = config;
        validateConfiguration();
    }

    @Bean(name = "tmdbRestTemplate")
    public RestTemplate tmdbRestTemplate(RestTemplateBuilder builder) {
        log.info("Configuring TMDb RestTemplate with timeouts: connect={}ms, read={}ms",
            config.getClient().getConnectTimeout(),
            config.getClient().getReadTimeout());

        return builder
            .requestFactory(this::clientHttpRequestFactory)
            .setConnectTimeout(Duration.ofMillis(config.getClient().getConnectTimeout()))
            .setReadTimeout(Duration.ofMillis(config.getClient().getReadTimeout()))
            .build();
    }

    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(config.getClient().getConnectTimeout());
        factory.setReadTimeout(config.getClient().getReadTimeout());
        return factory;
    }

    private void validateConfiguration() {
        if (config.getApi().getKey() == null || config.getApi().getKey().isBlank()) {
            throw new IllegalStateException(
                "TMDb API key is required when tmdb.api.enabled=true. " +
                "Please set TMDB_API_KEY environment variable or tmdb.api.key property. " +
                "Get your API key from: https://www.themoviedb.org/settings/api"
            );
        }

        // Only warn if it looks like a demo key
        if (config.getApi().getKey().contains("demo") || config.getApi().getKey().contains("replace")) {
            log.warn("⚠️  TMDb API key appears to be a placeholder. Movie import will not work until a valid API key is configured.");
            log.warn("⚠️  Get your free API key from: https://www.themoviedb.org/settings/api");
        } else if (config.getApi().getKey().length() < 20) {
            log.warn("TMDb API key appears to be invalid (too short). Expected at least 20 characters, got: {}",
                config.getApi().getKey().length());
        }

        log.info("TMDb client initialized successfully. Base URL: {}, Language: {}",
            config.getApi().getBaseUrl(),
            config.getApi().getLanguage());
    }
}

