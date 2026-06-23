package dev.shanternal.storage.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(InvalidContentLengthException.class)
    public ProblemDetail handleInvalidRequestException(InvalidContentLengthException e) {
        return buildProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Invalid Request",
                e.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleValidationException(ConstraintViolationException e) {
        return buildProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Validation Failed",
                e.getMessage()
        );
    }

    @ExceptionHandler(ObjectNotFoundException.class)
    public ProblemDetail handleObjectNotFoundException(ObjectNotFoundException e) {
        return buildProblemDetail(
                HttpStatus.NOT_FOUND,
                "Object not found",
                e.getMessage());
    }

    @ExceptionHandler(PayloadTooLargeException.class)
    public ProblemDetail handlePayloadTooLargeException(PayloadTooLargeException e) {
        return buildProblemDetail(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "Payload Too Large",
                e.getMessage());
    }

    @ExceptionHandler(UnsupportedMediaTypeException.class)
    public ProblemDetail handleInvalidRequestException(UnsupportedMediaTypeException e) {
        return buildProblemDetail(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Unsupported media type",
                e.getMessage());
    }

    @ExceptionHandler(StorageOperationException.class)
    public ProblemDetail handleStorageOperationException(StorageOperationException e) {
        return buildProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Storage Operation Error",
                e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleInternalServerError(Exception e) {
        return buildProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred");
    }

    private ProblemDetail buildProblemDetail(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
