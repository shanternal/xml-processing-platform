package dev.shanternal.request.service;

import dev.shanternal.request.dto.request.ProcessedRequestFilter;
import dev.shanternal.request.dto.response.ConvertXmlResult;
import dev.shanternal.request.dto.response.Page;
import dev.shanternal.request.dto.response.ProcessedRequestDetail;
import dev.shanternal.request.dto.response.ProcessedRequestSummary;
import org.springframework.stereotype.Service;

@Service
public class RequestService {

    public ConvertXmlResult convertXml(String xml) {
        return null;
    }

    public Page<ProcessedRequestSummary> getPage(ProcessedRequestFilter filter, int page, int size) {
        return null;
    }

    public ProcessedRequestDetail getById(Long id) {
        return null;
    }
}
