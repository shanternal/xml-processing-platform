package dev.shanternal.request.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(InvalidXmlException.class)
    public ProblemDetail handleInvalidXmlException(InvalidXmlException e) {
        return buildProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Invalid XML",
                e.getMessage()
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFoundException(ResourceNotFoundException e) {
        return buildProblemDetail(
                HttpStatus.NOT_FOUND,
                "Resource Not Found",
                e.getMessage()
        );
    }

    @ExceptionHandler(ConversionException.class)
    public ProblemDetail handleConversionException(ConversionException e) {
        return buildProblemDetail(
                HttpStatus.BAD_GATEWAY,
                "Conversion Service Error",
                e.getMessage()
        );
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
