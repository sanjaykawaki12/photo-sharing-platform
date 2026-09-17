package com.trizenai.photoshare.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Abstraction over object/file storage.
 *
 * Supports:
 * - Local storage
 * - AWS S3
 * - Cloudinary
 *
 * Actual image bytes are never stored in the database.
 */
public interface StorageService {

    /**
     * Stores the uploaded file and returns its storage location.
     */
    String store(
            MultipartFile file,
            String eventId
    ) throws IOException;

    /**
     * Converts the storage location into a URL
     * that the frontend can load.
     */
    String resolveUrl(
            String storageLocation
    );

    /**
     * Deletes the stored file/image.
     *
     * Implementations:
     * - Local -> deletes local file
     * - S3 -> deletes S3 object
     * - Cloudinary -> deletes Cloudinary image
     */
    void delete(
            String storageLocation
    ) throws IOException;
}