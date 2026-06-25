package dev.shanternal.request.dto.storage;

public record ConversionPayload(
        String canonicalXml,
        String targetJson
) {}
