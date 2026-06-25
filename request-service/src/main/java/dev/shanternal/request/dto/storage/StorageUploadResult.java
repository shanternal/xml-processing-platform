package dev.shanternal.request.dto.storage;

public record StorageUploadResult(
        String storageKey,
        long sizeInBytes) {
}
