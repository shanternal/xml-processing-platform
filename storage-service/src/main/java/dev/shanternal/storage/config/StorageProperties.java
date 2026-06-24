package dev.shanternal.storage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    private S3Properties s3 = new S3Properties();

    @Data
    public static class S3Properties {

        private String endpoint;

        private String region;

        private String accessKey;

        private String secretKey;

        private String bucketName;
    }
}

