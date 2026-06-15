package dev.shanternal.request.service;

import dev.shanternal.request.dto.request.ProcessedRequestFilter;
import dev.shanternal.request.dto.response.ConvertXmlResult;
import dev.shanternal.request.dto.response.Page;
import dev.shanternal.request.dto.response.ProcessedRequestDetail;
import dev.shanternal.request.dto.response.ProcessedRequestSummary;
import dev.shanternal.request.entity.ProcessedRequest;
import dev.shanternal.request.exception.ResourceNotFoundException;
import dev.shanternal.request.mapper.RequestMapper;
import dev.shanternal.request.repository.ProcessedRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RequestService {

    private final ProcessedRequestRepository processedRequestRepository;
    private final RequestMapper requestMapper;

    public ConvertXmlResult convertXml(String xml) {
        return null;
    }

    @Transactional(readOnly = true)
    public Page<ProcessedRequestSummary> getPage(ProcessedRequestFilter filter, int page, int size) {
        return processedRequestRepository.findAll(filter, page, size);
    }

    @Transactional(readOnly = true)
    public ProcessedRequestDetail getById(Long id) {
        ProcessedRequest processedRequest = processedRequestRepository.findByIdWithConversion(id)
                .orElseThrow(() -> new ResourceNotFoundException("Processed request with id=%d not found".formatted(id)));

        return requestMapper.toProcessedRequestDetail(processedRequest);
    }
}
