package dev.shanternal.storage.service;

import dev.shanternal.storage.exception.InvalidContentLengthException;
import dev.shanternal.storage.exception.UnsupportedMediaTypeException;
import dev.shanternal.storage.model.StorageUploadResult;
import dev.shanternal.storage.exception.ObjectNotFoundException;
import dev.shanternal.storage.exception.StorageOperationException;
import dev.shanternal.storage.model.StorageMetadata;
import dev.shanternal.storage.model.StorageObject;
import lombok.RequiredArgsConstructor;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.util.UUID;

@RequiredArgsConstructor
public class StorageService {

    private final S3Client s3Client;
    private final String bucketName;

    public StorageUploadResult upload(InputStream inputStream, long contentLength, String contentType) {
        ensureValidContentLength(contentLength);
        ensureValidContentType(contentType);

        String key = UUID.randomUUID().toString();

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .contentLength(contentLength)
                .build();

        try {
            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
        } catch (SdkException e) {
            throw new StorageOperationException("Error uploading an object to the storage [key=%s]: %s".formatted(key, e.getMessage()), e);
        }
        return new StorageUploadResult(key, contentLength);
    }

    public StorageMetadata getMetadata(String key) {
        try {
            HeadObjectResponse response = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());

            return new StorageMetadata(response.contentType(), response.contentLength());
        } catch (NoSuchKeyException e) {
            throw new ObjectNotFoundException("The object was not found in the storage: key=" + key);
        } catch (SdkException e) {
            throw new StorageOperationException("Error getting object metadata [key=%s]: %s".formatted(key, e.getMessage()), e);
        }
    }

    public StorageObject download(String key) {
        ResponseInputStream<GetObjectResponse> responseStream;
        try {
            responseStream = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
        } catch (NoSuchKeyException e) {
            throw new ObjectNotFoundException("The object was not found in the storage: key=" + key);
        } catch (SdkException e) {
            throw new StorageOperationException(
                    "Error downloading an object from storage [key=%s]: %s".formatted(key, e.getMessage()), e);
        }

        GetObjectResponse response = responseStream.response();
        StorageMetadata metadata = new StorageMetadata(response.contentType(), response.contentLength());

        return new StorageObject(responseStream, metadata);
    }

    public void delete(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
        } catch (SdkException e) {
            throw new StorageOperationException("Error deleting an object from storage [key=%s]: %s".formatted(key, e.getMessage()), e);
        }
    }

    private void ensureValidContentLength(long contentLength) {
        if (contentLength <= 0) {
            throw new InvalidContentLengthException("Content length must be positive: " + contentLength);
        }
    }

    private void ensureValidContentType(String contentType) {
        try {
            MediaType.parseMediaType(contentType);
        } catch (InvalidMediaTypeException e) {
            throw new UnsupportedMediaTypeException("Unsupported content type: " + contentType, e);
        }
    }
}
