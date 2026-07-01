package dev.shanternal.request.client.storage;

import dev.shanternal.request.dto.storage.ConversionPayload;
import dev.shanternal.request.dto.storage.StorageUploadResult;
import dev.shanternal.request.exception.StorageException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class HttpStorageClient implements StorageClient {

    private final RestClient storageRestClient;

    @Override
    @Retryable(
            retryFor = ResourceAccessException.class,
            maxAttemptsExpression = "${clients.storage.retry.max-attempts}",
            backoff = @Backoff(
                    delayExpression = "${clients.storage.retry.delay-ms}",
                    multiplierExpression = "${clients.storage.retry.multiplier}"
            )
    )
    public String upload(ConversionPayload payload) {
        try {
            StorageUploadResult created = storageRestClient.post()
                    .uri("/objects")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(StorageUploadResult.class);

            if (created == null || created.storageKey() == null) {
                throw new StorageException("Storage service returned an empty upload response");
            }
            return created.storageKey();

        } catch (RestClientResponseException e) {
            throw new StorageException("Storage service returned HTTP %d on upload".formatted(e.getStatusCode().value()), e);
        } catch (RestClientException e) {
            throw new StorageException("Storage client request failed", e);
        }
    }

    @Override
    @Retryable(
            retryFor = ResourceAccessException.class,
            maxAttemptsExpression = "${clients.storage.retry.max-attempts}",
            backoff = @Backoff(
                    delayExpression = "${clients.storage.retry.delay-ms}",
                    multiplierExpression = "${clients.storage.retry.multiplier}"
            )
    )
    public ConversionPayload download(String externalId) {
        try {
            ConversionPayload payload = storageRestClient.get()
                    .uri("/objects/{key}", externalId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(ConversionPayload.class);

            if (payload == null) {
                throw new StorageException("Storage service returned an empty object for externalId=%s".formatted(externalId));
            }
            return payload;

        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new StorageException("Object with externalId=%s not found in storage service".formatted(externalId), e);
            }
            throw new StorageException("Storage service returned HTTP %d on download".formatted(e.getStatusCode().value()), e);
        } catch (RestClientException e) {
            throw new StorageException("Failed to parse conversion payload for externalId=%s".formatted(externalId), e);
        }
    }

    @Recover
    public String recoverUpload(ResourceAccessException e, ConversionPayload payload) {
        throw new StorageException("Storage service unavailable after retry attempts (upload)", e);
    }

    @Recover
    public ConversionPayload recoverDownload(ResourceAccessException e, String externalId) {
        throw new StorageException("Storage service unavailable after retry attempts (download)", e);
    }
}
