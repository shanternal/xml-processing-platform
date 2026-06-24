package dev.shanternal.storage.exception;

public class InvalidContentLengthException extends RuntimeException {

    public InvalidContentLengthException(String message) {
        super(message);
    }

    public InvalidContentLengthException(String message, Throwable cause) {
        super(message, cause);
    }
}
