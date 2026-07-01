package dev.shanternal.request.migration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConversionResultMigrationScheduler {

    private final ConversionResultMigrationService migrationService;
    private final MigrationProperties properties;

    @Scheduled(
            initialDelayString = "${migration.storage.initial-delay-ms}",
            fixedDelayString = "${migration.storage.interval-ms}"
    )
    public void migrateBatch() {
        if (!properties.enabled()) {
            return;
        }

        int migrated = 0;
        for (int i = 0; i < properties.batchSize(); i++) {
            try {
                if (!migrationService.migrateOne()) {
                    break;
                }
                migrated++;
            } catch (Exception e) {
                log.error("Migration batch stopped after {} successful migration(s); remaining records will be retried on the next scheduled run", migrated, e);
                break;
            }
        }

        if (migrated > 0) {
            log.info("Migrated {} conversion_results row(s) to storage-service", migrated);
        }
    }
}
