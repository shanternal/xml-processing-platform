package dev.shanternal.storage.service;

import dev.shanternal.storage.exception.*;
import dev.shanternal.storage.model.StorageUploadResult;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@RequiredArgsConstructor
public class StorageService {

    private final S3Client s3Client;
    private final String bucketName;
    private final long maxUploadSizeBytes;

    public StorageUploadResult upload(InputStream inputStream, String contentType) {
        ensureValidContentType(contentType);
        byte[] body = getBufferBody(inputStream);

        String key = UUID.randomUUID().toString();

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        try {
            s3Client.putObject(request, RequestBody.fromBytes(body));
        } catch (SdkException e) {
            throw new StorageOperationException("Error uploading an object to the storage [key=%s]: %s".formatted(key, e.getMessage()), e);
        }

        return new StorageUploadResult(key, body.length);
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

    private byte[] getBufferBody(InputStream inputStream) {
        int limit = Math.toIntExact(maxUploadSizeBytes + 1);
        byte[] body;

        try {
            body = inputStream.readNBytes(limit);
        } catch (IOException e) {
            throw new StorageOperationException("Error reading request body: " + e.getMessage(), e);
        }

        if (body.length == 0) {
            throw new InvalidContentLengthException("Request body must not be empty");
        }

        if (body.length > maxUploadSizeBytes) {
            throw new PayloadTooLargeException("Uploaded content exceeds the maximum allowed size of %d bytes".formatted(maxUploadSizeBytes));
        }

        return body;
    }

    private void ensureValidContentType(String contentType) {
        try {
            MediaType.parseMediaType(contentType);
        } catch (InvalidMediaTypeException e) {
            throw new UnsupportedMediaTypeException("Unsupported content type: " + contentType, e);
        }
    }
}
