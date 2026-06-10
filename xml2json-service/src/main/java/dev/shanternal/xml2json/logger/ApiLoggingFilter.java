package dev.shanternal.xml2json.logger;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.time.Instant;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiLoggingFilter extends OncePerRequestFilter {

    private final ApiInteractionLogger apiInteractionLogger;

    private final DataSize maxRequestBodySize;

    public ApiLoggingFilter(
            ApiInteractionLogger apiInteractionLogger,
            @Value("${app.logging.max-request-body-size:10MB}") DataSize maxRequestBodySize
    ) {
        this.apiInteractionLogger = apiInteractionLogger;
        this.maxRequestBodySize = maxRequestBodySize;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        int maxSize = Math.toIntExact(Math.min(maxRequestBodySize.toBytes(), Integer.MAX_VALUE));
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request, maxSize);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        Instant timestamp = Instant.now();
        long startedAt = System.nanoTime();

        Throwable failure = null;

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);

        } catch (Throwable e) {
            failure = e;
            throw e;

        } finally {

            long durationInMs = (System.nanoTime() - startedAt) / 1_000_000;

            apiInteractionLogger.log(requestWrapper, responseWrapper, timestamp, durationInMs, failure);

            responseWrapper.copyBodyToResponse();
        }
    }
}
