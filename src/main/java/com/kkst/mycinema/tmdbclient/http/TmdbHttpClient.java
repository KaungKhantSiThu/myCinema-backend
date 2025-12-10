package com.kkst.mycinema.tmdbclient.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kkst.mycinema.tmdbclient.config.TmdbClientConfig;
import com.kkst.mycinema.tmdbclient.exception.TmdbApiException;
import com.kkst.mycinema.tmdbclient.exception.TmdbAuthenticationException;
import com.kkst.mycinema.tmdbclient.exception.TmdbResourceNotFoundException;
import com.kkst.mycinema.tmdbclient.model.TmdbErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * HTTP client for making requests to TMDb API.
 * Handles authentication, error handling, and retry logic.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "tmdb.api.enabled", havingValue = "true")
public class TmdbHttpClient {

    private final RestTemplate restTemplate;
    private final TmdbClientConfig config;
    private final ObjectMapper objectMapper;

    public TmdbHttpClient(@Qualifier("tmdbRestTemplate") RestTemplate restTemplate,
                          TmdbClientConfig config,
                          ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.config = config;
        this.objectMapper = objectMapper;
    }

    /**
     * Execute a GET request to TMDb API.
     *
     * @param endpoint     The API endpoint (without base URL)
     * @param queryParams  Query parameters to include
     * @param responseType The expected response type
     * @param <T>          Response type
     * @return The response object
     */
    public <T> T get(String endpoint, Map<String, String> queryParams, ParameterizedTypeReference<T> responseType) {
        return executeWithRetry(() -> executeGet(endpoint, queryParams, responseType));
    }

    /**
     * Execute a GET request to TMDb API with class-based response type.
     *
     * @param endpoint     The API endpoint (without base URL)
     * @param queryParams  Query parameters to include
     * @param responseType The expected response class
     * @param <T>          Response type
     * @return The response object
     */
    public <T> T get(String endpoint, Map<String, String> queryParams, Class<T> responseType) {
        return executeWithRetry(() -> executeGet(endpoint, queryParams, responseType));
    }

    private <T> T executeGet(String endpoint, Map<String, String> queryParams, ParameterizedTypeReference<T> responseType) {
        String url = buildUrl(endpoint, queryParams);
        HttpHeaders headers = createHeaders();
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        log.debug("Executing GET request to: {}", url);

        try {
            ResponseEntity<T> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                responseType
            );

            log.debug("Request successful. Status: {}", response.getStatusCode());
            return response.getBody();

        } catch (HttpStatusCodeException e) {
            handleHttpError(e);
            throw new TmdbApiException("Request failed", e);
        } catch (RestClientException e) {
            log.error("REST client error: {}", e.getMessage(), e);
            throw new TmdbApiException("Failed to execute request: " + e.getMessage(), e);
        }
    }

    private <T> T executeGet(String endpoint, Map<String, String> queryParams, Class<T> responseType) {
        String url = buildUrl(endpoint, queryParams);
        HttpHeaders headers = createHeaders();
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        log.debug("Executing GET request to: {}", url);

        try {
            ResponseEntity<T> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                responseType
            );

            log.debug("Request successful. Status: {}", response.getStatusCode());
            return response.getBody();

        } catch (HttpStatusCodeException e) {
            handleHttpError(e);
            throw new TmdbApiException("Request failed", e);
        } catch (RestClientException e) {
            log.error("REST client error: {}", e.getMessage(), e);
            throw new TmdbApiException("Failed to execute request: " + e.getMessage(), e);
        }
    }

    private String buildUrl(String endpoint, Map<String, String> queryParams) {
        UriComponentsBuilder builder = UriComponentsBuilder
            .fromHttpUrl(config.getApi().getBaseUrl())
            .path(endpoint)
            .queryParam("api_key", config.getApi().getKey());

        if (queryParams != null) {
            queryParams.forEach(builder::queryParam);
        }

        return builder.toUriString();
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json");
        return headers;
    }

    private void handleHttpError(HttpStatusCodeException e) {
        HttpStatusCode statusCode = e.getStatusCode();
        String responseBody = e.getResponseBodyAsString();

        log.error("HTTP error: {} - {}", statusCode, responseBody);

        try {
            TmdbErrorResponse errorResponse = objectMapper.readValue(responseBody, TmdbErrorResponse.class);

            if (statusCode.value() == 401 || statusCode.value() == 403) {
                throw new TmdbAuthenticationException("Authentication failed: " + errorResponse.getStatusMessage());
            } else if (statusCode.value() == 404) {
                throw new TmdbResourceNotFoundException("Resource not found: " + errorResponse.getStatusMessage());
            } else {
                throw new TmdbApiException(errorResponse.getStatusCode(), errorResponse.getStatusMessage());
            }
        } catch (TmdbApiException tmdbException) {
            // Re-throw TMDb-specific exceptions
            throw tmdbException;
        } catch (Exception ex) {
            // If we can't parse the error response, throw a generic exception
            log.warn("Failed to parse error response: {}", ex.getMessage());
            throw new TmdbApiException(statusCode.value(), e.getMessage());
        }
    }

    private <T> T executeWithRetry(RetryableOperation<T> operation) {
        int maxAttempts = config.getRetry().getMaxAttempts();
        long backoffDelay = config.getRetry().getBackoffDelay();
        double backoffMultiplier = config.getRetry().getBackoffMultiplier();

        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return operation.execute();
            } catch (TmdbAuthenticationException | TmdbResourceNotFoundException e) {
                // Don't retry authentication or not found errors
                throw e;
            } catch (Exception e) {
                lastException = e;

                if (attempt < maxAttempts) {
                    long delay = (long) (backoffDelay * Math.pow(backoffMultiplier, attempt - 1));
                    log.warn("Request failed (attempt {}/{}), retrying in {}ms: {}",
                        attempt, maxAttempts, delay, e.getMessage());

                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new TmdbApiException("Request interrupted", ie);
                    }
                } else {
                    log.error("Request failed after {} attempts", maxAttempts);
                }
            }
        }

        throw new TmdbApiException("Request failed after " + maxAttempts + " attempts", lastException);
    }

    @FunctionalInterface
    private interface RetryableOperation<T> {
        T execute() throws Exception;
    }
}

