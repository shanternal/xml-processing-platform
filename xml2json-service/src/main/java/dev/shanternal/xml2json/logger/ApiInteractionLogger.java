package dev.shanternal.xml2json.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Supplier;

@Component
public class ApiInteractionLogger {

    private static final Logger API_LOGGER = LoggerFactory.getLogger("API_INTERACTION");

    private static final String MESSAGE_TEMPLATE =
            "timestamp=%s method=%s uri=%s query=%s requestHeaders=%s requestBody=%s " +
                    "status=%s responseHeaders=%s responseBody=%s durationMs=%s";

    public void log(
            ContentCachingRequestWrapper request,
            ContentCachingResponseWrapper response,
            Instant timestamp,
            long durationMs,
            Throwable failure
    ) {
        Supplier<String> message = () -> buildMessage(request, response, timestamp, durationMs);

        if (response.getStatus() >= 500) {
            API_LOGGER.atError().setCause(failure).log(message);
        } else if (response.getStatus() >= 400) {
            API_LOGGER.atWarn().log(message);
        } else {
            API_LOGGER.atInfo().log(message);
        }
    }

    private String buildMessage(
            ContentCachingRequestWrapper request,
            ContentCachingResponseWrapper response,
            Instant timestamp,
            long durationMs
    ) {
        return MESSAGE_TEMPLATE.formatted(
                timestamp,
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                extractRequestHeaders(request),
                sanitizeBody(extractBody(request.getContentAsByteArray(), request.getCharacterEncoding())),
                response.getStatus(),
                extractResponseHeaders(response),
                sanitizeBody(extractBody(response.getContentAsByteArray(), response.getCharacterEncoding())),
                durationMs
        );
    }

    private HttpHeaders extractRequestHeaders(ContentCachingRequestWrapper request) {
        HttpHeaders headers = new HttpHeaders();
        Collections.list(request.getHeaderNames())
                .forEach(name -> headers.put(name, Collections.list(request.getHeaders(name))));
        return headers;
    }

    private HttpHeaders extractResponseHeaders(ContentCachingResponseWrapper response) {
        HttpHeaders headers = new HttpHeaders();
        response.getHeaderNames()
                .forEach(name -> headers.put(name, new ArrayList<>(response.getHeaders(name))));
        return headers;
    }

    private String extractBody(byte[] bytes, String encoding) {
        return new String(bytes, charsetOf(encoding));
    }

    private String sanitizeBody(String body) {
        if (body == null || body.isEmpty()) {
            return body;
        }
        return body.replace("\n", "\\n").replace("\r", "\\r");
    }

    private Charset charsetOf(String encoding) {
        if (encoding == null) return StandardCharsets.UTF_8;
        try {
            return Charset.forName(encoding);
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }
}