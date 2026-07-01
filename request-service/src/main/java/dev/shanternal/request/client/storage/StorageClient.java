package dev.shanternal.request.client.storage;

import dev.shanternal.request.dto.storage.ConversionPayload;

public interface StorageClient {

    String upload(ConversionPayload payload);

    ConversionPayload download(String externalId);
}
