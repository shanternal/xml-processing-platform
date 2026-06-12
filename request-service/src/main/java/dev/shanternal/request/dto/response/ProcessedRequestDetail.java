package dev.shanternal.request.dto.response;

import com.fasterxml.jackson.annotation.JsonRawValue;

import java.time.OffsetDateTime;

public record ProcessedRequestDetail(
        Long id,
        String canonicalXml,
        @JsonRawValue
        String json,
        OffsetDateTime requestedAt,
        Long processingTimeMs,
        Integer xmlTagsCount,
        Integer jsonKeysCount
) {}
