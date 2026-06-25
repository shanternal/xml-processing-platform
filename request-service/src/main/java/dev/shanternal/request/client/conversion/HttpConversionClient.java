package dev.shanternal.request.client.conversion;

import dev.shanternal.request.exception.ConversionException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class HttpConversionClient implements ConversionClient {

    private final RestClient conversionRestClient;

    @Override
    @Retryable(
            retryFor = ResourceAccessException.class,
            maxAttemptsExpression = "${clients.conversion.retry.max-attempts}",
            backoff = @Backoff(
                    delayExpression = "${clients.conversion.retry.delay-ms}",
                    multiplierExpression = "${clients.conversion.retry.multiplier}"
            )
    )
    public String convert(String xml) {
        try {
            return conversionRestClient.post()
                    .uri("/xml2json")
                    .contentType(MediaType.APPLICATION_XML)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(xml)
                    .retrieve()
                    .body(String.class);

        } catch (RestClientResponseException e) {
            throw new ConversionException("Conversion service returned HTTP %d".formatted(e.getStatusCode().value()), e);
        }
    }

    @Recover
    public String recoverConvert(ResourceAccessException e, String xml) {
        throw new ConversionException("Conversion service unavailable after retry attempts", e);
    }
}
