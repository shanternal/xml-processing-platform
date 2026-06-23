package dev.shanternal.storage.controller;

import dev.shanternal.storage.model.StorageUploadResult;
import dev.shanternal.storage.model.StorageMetadata;
import dev.shanternal.storage.model.StorageObject;
import dev.shanternal.storage.service.StorageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.InputStream;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/objects")
@RequiredArgsConstructor
@Validated
public class StorageController {

    private final StorageService storageService;

    @PostMapping(consumes = MediaType.ALL_VALUE)
    public ResponseEntity<StorageUploadResult> upload(HttpServletRequest request, InputStream body) {
        long contentLength = request.getContentLengthLong();
        String contentType = request.getContentType();

        StorageUploadResult result = storageService.upload(body, contentLength, contentType);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                    .path("/{key}")
                    .buildAndExpand(result.storageKey())
                    .toUri();

        return ResponseEntity.created(location).body(result);
    }

    @GetMapping("/{key}")
    public ResponseEntity<StreamingResponseBody> download(@PathVariable @NotBlank String key) {
        StorageObject object = storageService.download(key);

        StreamingResponseBody body = outputStream -> {
            try (var content = object.content()) {
                content.transferTo(outputStream);
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(object.metadata().mediaType()))
                .contentLength(object.metadata().sizeInBytes())
                .body(body);
    }

    @RequestMapping(value = "/{key}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> getMetadata(@PathVariable @NotBlank String key) {
        StorageMetadata metadata = storageService.getMetadata(key);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(metadata.mediaType()))
                .contentLength(metadata.sizeInBytes())
                .build();
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> delete(@PathVariable @NotBlank String key) {
        storageService.delete(key);
        return ResponseEntity.noContent().build();
    }
}
