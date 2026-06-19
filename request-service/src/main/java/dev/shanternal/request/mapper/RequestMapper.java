package dev.shanternal.request.mapper;

import dev.shanternal.request.dto.response.ConvertXmlResult;
import dev.shanternal.request.dto.response.ProcessedRequestDetail;
import dev.shanternal.request.entity.ConversionResult;
import dev.shanternal.request.entity.ProcessedRequest;
import org.springframework.stereotype.Component;

@Component
public class RequestMapper {

    public ConvertXmlResult toConvertXmlResult(ProcessedRequest request) {
        ConversionResult conversion = request.getConversionResult();

        return new ConvertXmlResult(
                request.getId(),
                conversion.getTargetJson()
        );
    }

    public ProcessedRequestDetail toProcessedRequestDetail(ProcessedRequest request) {
        ConversionResult conversion = request.getConversionResult();

        return new ProcessedRequestDetail(
                request.getId(),
                conversion.getCanonicalXml(),
                conversion.getTargetJson(),
                request.getRequestedAt(),
                request.getProcessingTimeMs(),
                conversion.getXmlTagsCount(),
                conversion.getJsonKeysCount()
        );
    }
}
