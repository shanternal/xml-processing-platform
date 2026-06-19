package dev.shanternal.request.dto.response;

import java.time.OffsetDateTime;

public record ProcessedRequestSummary(
        Long id,
        OffsetDateTime requestedAt,
        Long processingTimeMs,
        Integer xmlTagsCount,
        Integer jsonKeysCount
) {
}