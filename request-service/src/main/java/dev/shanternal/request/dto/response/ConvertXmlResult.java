package dev.shanternal.request.dto.response;

import com.fasterxml.jackson.annotation.JsonRawValue;

public record ConvertXmlResult(
        Long id,
        @JsonRawValue
        String json
) {}