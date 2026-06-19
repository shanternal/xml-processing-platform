package dev.shanternal.request.dto.request;

import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;

public record ProcessedRequestFilter(

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        OffsetDateTime requestedAtFrom,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        OffsetDateTime requestedAtTo,

        @PositiveOrZero(message = "Minimum processing time cannot be negative")
        Long processingTimeMsMin,

        @PositiveOrZero(message = "Maximum processing time cannot be negative")
        Long processingTimeMsMax,

        @Positive(message = "Minimum XML tags count must be at least 1")
        Integer xmlTagsCountMin,

        @Positive(message = "Maximum XML tags count must be at least 1")
        Integer xmlTagsCountMax,

        @PositiveOrZero(message = "Minimum JSON keys count cannot be negative")
        Integer jsonKeysCountMin,

        @PositiveOrZero(message = "Maximum JSON keys count cannot be negative")
        Integer jsonKeysCountMax
) {

    @AssertTrue(message = "Start date must be before or equal to the end date")
    public boolean isRequestedAtRangeValid() {
        return isRangeValid(requestedAtFrom, requestedAtTo);
    }

    @AssertTrue(message = "Minimum processing time cannot exceed maximum processing time")
    public boolean isProcessingTimeRangeValid() {
        return isRangeValid(processingTimeMsMin, processingTimeMsMax);
    }

    @AssertTrue(message = "Minimum XML tags count cannot exceed maximum XML tags count")
    public boolean isXmlTagsCountRangeValid() {
        return isRangeValid(xmlTagsCountMin, xmlTagsCountMax);
    }

    @AssertTrue(message = "Minimum JSON keys count cannot exceed maximum JSON keys count")
    public boolean isJsonKeysCountRangeValid() {
        return isRangeValid(jsonKeysCountMin, jsonKeysCountMax);
    }

    private static <T extends Comparable<? super T>> boolean isRangeValid(T from, T to) {
        return from == null || to == null || from.compareTo(to) <= 0;
    }
}