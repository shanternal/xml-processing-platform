package dev.shanternal.xml2json.logger;

import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.time.Instant;

@Component
public class ApiInteractionLogger {
    public void log(
            ContentCachingRequestWrapper request,
            ContentCachingResponseWrapper response,
            Instant timestamp,
            long durationMs,
            Throwable failure
    ) {}
}
