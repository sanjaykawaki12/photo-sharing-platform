package com.trizenai.photoshare.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@ConditionalOnProperty(
        name = "app.storage.provider",
        havingValue = "local",
        matchIfMissing = true
)
public class LocalStorageService implements StorageService {

    private final Path root;
    private final String publicBaseUrl;

    public LocalStorageService(
            @Value("${app.storage.local.dir:./uploads}") String dir,
            @Value("${app.storage.local.public-base-url:http://localhost:8080/uploads}") String publicBaseUrl
    ) {
        this.root = Paths.get(dir);
        this.publicBaseUrl = publicBaseUrl;
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(root);
    }

    // =========================================================
    // STORE
    // =========================================================

    @Override
    public String store(
            MultipartFile file,
            String eventId
    ) throws IOException {

        String safeExt = getExtension(
                file.getOriginalFilename()
        );

        String key =
                "events/"
                        + eventId
                        + "/"
                        + UUID.randomUUID()
                        + safeExt;

        Path destination = root.resolve(key);

        Files.createDirectories(
                destination.getParent()
        );

        Files.copy(
                file.getInputStream(),
                destination,
                StandardCopyOption.REPLACE_EXISTING
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

        return publicBaseUrl
                + "/"
                + storageLocation;
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

        /*
         * Protect against path traversal.
         *
         * Example of dangerous input:
         * ../../some-file
         *
         * We only allow files inside the configured
         * storage root directory.
         */

        Path target = root.resolve(
                storageLocation
        ).normalize();

        Path normalizedRoot = root
                .toAbsolutePath()
                .normalize();

        Path normalizedTarget = target
                .toAbsolutePath()
                .normalize();

        if (!normalizedTarget.startsWith(
                normalizedRoot
        )) {

            throw new IOException(
                    "Invalid storage location"
            );
        }

        // Delete only if the file actually exists.
        Files.deleteIfExists(
                normalizedTarget
        );
    }

    // =========================================================
    // FILE EXTENSION
    // =========================================================

    private String getExtension(
            String originalFilename
    ) {

        if (originalFilename == null
                || !originalFilename.contains(".")) {
            return "";
        }

        return originalFilename.substring(
                originalFilename.lastIndexOf('.')
        );
    }
}