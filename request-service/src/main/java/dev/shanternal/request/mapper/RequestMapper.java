package dev.shanternal.request.mapper;

import dev.shanternal.request.dto.response.ConvertXmlResult;
import dev.shanternal.request.dto.response.ProcessedRequestDetail;
import dev.shanternal.request.dto.storage.ConversionPayload;
import dev.shanternal.request.entity.ConversionResult;
import dev.shanternal.request.entity.ProcessedRequest;
import org.springframework.stereotype.Component;

@Component
public class RequestMapper {

    public ConvertXmlResult toConvertXmlResult(ProcessedRequest request, ConversionPayload payload) {
        return new ConvertXmlResult(
                request.getId(),
                payload.targetJson()
        );
    }

    public ProcessedRequestDetail toProcessedRequestDetail(ProcessedRequest request, ConversionPayload payload) {
        ConversionResult conversion = request.getConversionResult();

        return new ProcessedRequestDetail(
                request.getId(),
                payload.canonicalXml(),
                payload.targetJson(),
                request.getRequestedAt(),
                request.getProcessingTimeMs(),
                conversion.getXmlTagsCount(),
                conversion.getJsonKeysCount()
        );
    }
}
