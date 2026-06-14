package dev.shanternal.request.controller;

import dev.shanternal.request.dto.request.ProcessedRequestFilter;
import dev.shanternal.request.dto.response.ConvertXmlResult;
import dev.shanternal.request.dto.response.Page;
import dev.shanternal.request.dto.response.ProcessedRequestDetail;
import dev.shanternal.request.dto.response.ProcessedRequestSummary;
import dev.shanternal.request.service.RequestService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class RequestController {

    private final RequestService requestService;

    @PostMapping(
            value = "/request",
            consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ConvertXmlResult> convertXml(@RequestBody @NotBlank String xml) {
        ConvertXmlResult response = requestService.convertXml(xml);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping(value = "/page", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<ProcessedRequestSummary>> getPage(
            @Valid @ModelAttribute ProcessedRequestFilter filter,
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "20") @Positive int size
    ) {
        Page<ProcessedRequestSummary> result = requestService.getPage(filter, page, size);
        return ResponseEntity.ok(result);
    }

    @GetMapping(value = "/request/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProcessedRequestDetail> getById(
            @PathVariable @Positive Long id
    ) {
        ProcessedRequestDetail response = requestService.getById(id);
        return ResponseEntity.ok(response);
    }
}
