package com.kkst.mycinema.exception;

/**
 * Exception thrown when external API calls fail (e.g., TMDb, OMDB).
 * This exception indicates a problem with the external service, not with our application logic.
 */
public class ExternalApiException extends RuntimeException {

    private final String apiName;
    private final String errorCode;

    public ExternalApiException(String apiName, String message) {
        super(message);
        this.apiName = apiName;
        this.errorCode = "EXTERNAL_API_ERROR";
    }

    public ExternalApiException(String apiName, String message, Throwable cause) {
        super(message, cause);
        this.apiName = apiName;
        this.errorCode = "EXTERNAL_API_ERROR";
    }

    public ExternalApiException(String apiName, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.apiName = apiName;
        this.errorCode = errorCode;
    }

    public String getApiName() {
        return apiName;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

