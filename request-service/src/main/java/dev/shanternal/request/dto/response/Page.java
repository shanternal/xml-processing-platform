package dev.shanternal.request.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record Page<T>(
        List<T> content,
        int page,
        int size,
        long totalElements
) {
    @JsonProperty("totalPages")
    public int getTotalPages() {
        if (size == 0) {
            return 1;
        }
        return (int) Math.ceil((double) totalElements / size);
    }
}