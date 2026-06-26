package dev.shanternal.request.migration;

import dev.shanternal.request.client.storage.StorageClient;
import dev.shanternal.request.dto.storage.ConversionPayload;
import dev.shanternal.request.entity.ConversionResult;
import dev.shanternal.request.repository.ConversionResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConversionResultMigrationService {

    private final ConversionResultRepository conversionResultRepository;
    private final StorageClient storageClient;

    @Transactional(timeout = 20)
    public boolean migrateOne() {
        Optional<ConversionResult> claimed = conversionResultRepository.claimNextForMigration();

        if (claimed.isEmpty()) {
            return false;
        }

        ConversionResult conversionResult = claimed.get();

        ConversionPayload payload = new ConversionPayload(
                conversionResult.getCanonicalXml(),
                conversionResult.getTargetJson()
        );

        String externalId = storageClient.upload(payload);

        conversionResult.markAsMigrated(externalId);

        return true;
    }
}
