package com.trizenai.photoshare.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@ConditionalOnProperty(
        name = "app.storage.provider",
        havingValue = "s3"
)
public class S3StorageService implements StorageService {

    private final S3Client s3Client;
    private final String bucket;
    private final String region;

    public S3StorageService(
            @Value("${app.storage.s3.bucket}") String bucket,
            @Value("${app.storage.s3.region}") String region
    ) {
        this.bucket = bucket;
        this.region = region;

        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .build();
    }

    // =========================================================
    // STORE
    // =========================================================

    @Override
    public String store(
            MultipartFile file,
            String eventId
    ) throws IOException {

        String key =
                "events/"
                        + eventId
                        + "/"
                        + UUID.randomUUID()
                        + "-"
                        + file.getOriginalFilename();

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(file.getContentType())
                        .build(),
                RequestBody.fromInputStream(
                        file.getInputStream(),
                        file.getSize()
                )
        );

        return key;
    }

    // =========================================================
    // RESOLVE URL
    // =========================================================

    @Override
    public String resolveUrl(
            String storageLocation
    ) {

        if (storageLocation == null
                || storageLocation.isBlank()) {
            return "";
        }

        // Simple public URL.
        // For private buckets, use S3Presigner instead.
        return "https://%s.s3.%s.amazonaws.com/%s"
                .formatted(
                        bucket,
                        region,
                        storageLocation
                );
    }

    // =========================================================
    // DELETE
    // =========================================================

    @Override
    public void delete(
            String storageLocation
    ) throws IOException {

        if (storageLocation == null
                || storageLocation.isBlank()) {
            return;
        }

        try {

            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(bucket)
                            .key(storageLocation)
                            .build()
            );

        } catch (Exception ex) {

            throw new IOException(
                    "Failed to delete object from S3",
                    ex
            );
        }
    }
}