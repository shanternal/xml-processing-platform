package dev.shanternal.storage.exception;

public class UnsupportedMediaTypeException extends RuntimeException {
    public UnsupportedMediaTypeException(String message) {
        super(message);
    }
    public UnsupportedMediaTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}
