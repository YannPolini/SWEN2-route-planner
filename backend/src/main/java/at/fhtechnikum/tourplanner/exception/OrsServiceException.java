package at.fhtechnikum.tourplanner.exception;

/**
 * Business-layer exception for any failure of the OpenRouteService integration.
 * Wraps low-level HTTP/JSON errors so callers never see implementation-specific
 * exceptions (own-layer-exception rule).
 */
public class OrsServiceException extends RuntimeException {
    public OrsServiceException(String message) {
        super(message);
    }

    public OrsServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
