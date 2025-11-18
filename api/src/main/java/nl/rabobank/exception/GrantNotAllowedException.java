package nl.rabobank.exception;

public class GrantNotAllowedException extends RuntimeException {
    public GrantNotAllowedException(String message) {
        super(message);
    }
}
