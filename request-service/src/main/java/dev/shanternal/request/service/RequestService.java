package dev.shanternal.request.service;

import dev.shanternal.request.client.conversion.ConversionClient;
import dev.shanternal.request.dto.request.ProcessedRequestFilter;
import dev.shanternal.request.dto.response.ConvertXmlResult;
import dev.shanternal.request.dto.response.Page;
import dev.shanternal.request.dto.response.ProcessedRequestDetail;
import dev.shanternal.request.dto.response.ProcessedRequestSummary;
import dev.shanternal.request.entity.ConversionResult;
import dev.shanternal.request.entity.ProcessedRequest;
import dev.shanternal.request.exception.ConversionException;
import dev.shanternal.request.exception.InvalidXmlException;
import dev.shanternal.request.exception.ResourceNotFoundException;
import dev.shanternal.request.mapper.RequestMapper;
import dev.shanternal.request.processor.JsonProcessor;
import dev.shanternal.request.processor.XmlProcessor;
import dev.shanternal.request.repository.ConversionResultRepository;
import dev.shanternal.request.repository.ProcessedRequestRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.w3c.dom.Document;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RequestService {

    private final ProcessedRequestRepository processedRequestRepository;
    private final ConversionResultRepository conversionResultRepository;
    private final ConversionClient conversionClient;
    private final RequestMapper requestMapper;
    private final XmlProcessor xmlProcessor;
    private final JsonProcessor jsonProcessor;
    private final TransactionTemplate writeTransactionTemplate;
    private final TransactionTemplate readTransactionTemplate;

    public ConvertXmlResult convertXml(String xml) {
        long startTime = System.nanoTime();

        Document xmlDocument;
        try {
            xmlDocument = xmlProcessor.parseXml(xml);
        } catch (IllegalArgumentException e) {
            throw new InvalidXmlException("Invalid XML", e);
        }

        String canonicalXml = xmlProcessor.canonicalizeXml(xmlDocument);
        String xmlHash = DigestUtils.sha256Hex(canonicalXml);
        int xmlTagsCount = xmlProcessor.countXmlTags(xmlDocument);

        ConversionResult conversionResult = findOrCreateConversionResult(xml, xmlHash, canonicalXml, xmlTagsCount);

        long processingTimeMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        ProcessedRequest processedRequest = writeTransactionTemplate.execute(status ->
                processedRequestRepository.save(
                        ProcessedRequest.builder()
                                .conversionResult(conversionResult)
                                .requestedAt(OffsetDateTime.now(ZoneOffset.UTC))
                                .processingTimeMs(processingTimeMs)
                                .build()
                )
        );

        return requestMapper.toConvertXmlResult(processedRequest);
    }

    private ConversionResult findOrCreateConversionResult(String xml, String xmlHash, String canonicalXml, int xmlTagsCount) {
        Optional<ConversionResult> cached = Optional.ofNullable(
                readTransactionTemplate.execute(status ->
                conversionResultRepository.findByXmlHash(xmlHash).orElse(null)));

        return cached.orElseGet(() -> createConversionResult(xml, xmlHash, canonicalXml, xmlTagsCount));
    }

    private ConversionResult createConversionResult(String xml, String xmlHash, String canonicalXml, int xmlTagsCount) {
        String json = conversionClient.convert(xml);
        int jsonKeysCount;
        try {
            jsonKeysCount = jsonProcessor.countKeys(json);
        } catch (IllegalArgumentException e) {
            throw new ConversionException("Conversion service returned malformed JSON", e);
        }

        return writeTransactionTemplate.execute(status ->
                conversionResultRepository.saveIfAbsent(
                        ConversionResult.builder()
                                .xmlHash(xmlHash)
                                .canonicalXml(canonicalXml)
                                .xmlTagsCount(xmlTagsCount)
                                .targetJson(json)
                                .jsonKeysCount(jsonKeysCount)
                                .build()
                ));
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