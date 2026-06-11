package dev.shanternal.request.client;

import dev.shanternal.request.exception.ConversionException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class HttpConversionClient implements ConversionClient {

    private final RestClient conversionRestClient;

    @Override
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

        } catch (ResourceAccessException e) {
            throw new ConversionException("Unable to reach conversion service", e);
        }
    }
}