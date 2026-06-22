package at.fhtechnikum.tourplanner.exception;

/** Business-layer exception: a requested entity (e.g. a Tour) does not exist. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
