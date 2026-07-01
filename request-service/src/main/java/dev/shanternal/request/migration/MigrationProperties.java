package dev.shanternal.request.migration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "migration.storage")
public record MigrationProperties(
        boolean enabled,
        long intervalMs,
        long initialDelayMs,
        int batchSize
) {}