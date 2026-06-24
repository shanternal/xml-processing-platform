package dev.shanternal.storage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@Data
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    private S3Properties s3 = new S3Properties();

    private DataSize maxUploadSize;

    @Data
    public static class S3Properties {

        private String endpoint;

        private String region;

        private String accessKey;

        private String secretKey;

        private String bucketName;
    }
}

