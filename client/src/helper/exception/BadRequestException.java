package helper.exception;

/**
 * If a bad request was made this exception will be thrown
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
