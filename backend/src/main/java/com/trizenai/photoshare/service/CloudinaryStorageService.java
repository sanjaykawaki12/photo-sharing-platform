package com.trizenai.photoshare.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@ConditionalOnProperty(
        name = "app.storage.provider",
        havingValue = "cloudinary",
        matchIfMissing = true
)
public class CloudinaryStorageService implements StorageService {

    private final Cloudinary cloudinary;

    public CloudinaryStorageService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    // =========================================================
    // STORE
    // =========================================================

    @Override
    public String store(
            MultipartFile file,
            String eventId
    ) throws IOException {

        String publicId =
                "events/"
                        + eventId
                        + "/"
                        + System.currentTimeMillis();

        try {

            Map<?, ?> result =
                    cloudinary.uploader().upload(
                            file.getBytes(),
                            ObjectUtils.asMap(
                                    "public_id", publicId,
                                    "resource_type", "image"
                            )
                    );

            Object secureUrl = result.get("secure_url");

            if (secureUrl == null) {
                throw new IOException(
                        "Cloudinary did not return a secure URL"
                );
            }

            return secureUrl.toString();

        } catch (Exception e) {

            throw new IOException(
                    "Cloudinary upload failed: "
                            + e.getMessage(),
                    e
            );
        }
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

        /*
         * New Cloudinary photos:
         * https://res.cloudinary.com/...
         *
         * Old local photos:
         * events/3/xxxxx.jpg
         *
         * Keep both working.
         */
        if (storageLocation.startsWith("http://")
                || storageLocation.startsWith("https://")) {

            return storageLocation;
        }

        return "http://localhost:8080/uploads/"
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
         * Old local-storage photos are stored as:
         *
         * events/3/uuid.jpg
         *
         * They are NOT Cloudinary images.
         * So don't try to delete them from Cloudinary.
         */
        if (!storageLocation.startsWith("http://")
                && !storageLocation.startsWith("https://")) {
            return;
        }

        try {

            /*
             * Cloudinary URL example:
             *
             * https://res.cloudinary.com/demo/image/upload/
             * v123456789/events/3/1750000000000.jpg
             *
             * We need:
             *
             * events/3/1750000000000
             *
             * as the public_id.
             */

            String publicId =
                    extractPublicId(storageLocation);

            if (publicId == null
                    || publicId.isBlank()) {

                throw new IOException(
                        "Could not determine Cloudinary public ID"
                );
            }

            Map<?, ?> result =
                    cloudinary.uploader().destroy(
                            publicId,
                            ObjectUtils.asMap(
                                    "resource_type", "image",
                                    "invalidate", true
                            )
                    );

            Object resultStatus = result.get("result");

            if (resultStatus != null
                    && "ok".equalsIgnoreCase(
                    resultStatus.toString())) {
                return;
            }

            /*
             * Cloudinary can return "not found" if the image
             * was already deleted. Treat that as successful
             * deletion so DB cleanup can continue.
             */
            if (resultStatus != null
                    && "not found".equalsIgnoreCase(
                    resultStatus.toString())) {
                return;
            }

            throw new IOException(
                    "Cloudinary delete failed: "
                            + result
            );

        } catch (IOException e) {

            throw e;

        } catch (Exception e) {

            throw new IOException(
                    "Cloudinary delete failed: "
                            + e.getMessage(),
                    e
            );
        }
    }

    // =========================================================
    // EXTRACT CLOUDINARY PUBLIC ID
    // =========================================================

    private String extractPublicId(
            String cloudinaryUrl
    ) {

        try {

            /*
             * Find the upload section.
             */
            int uploadIndex =
                    cloudinaryUrl.indexOf("/upload/");

            if (uploadIndex == -1) {
                return null;
            }

            String path =
                    cloudinaryUrl.substring(
                            uploadIndex + "/upload/".length()
                    );

            /*
             * Remove transformation section if present.
             *
             * Example:
             * c_fill,w_500/v123/events/3/image.jpg
             *
             * We only want:
             * events/3/image
             */

            String[] parts =
                    path.split("/");

            int startIndex = 0;

            /*
             * Skip Cloudinary transformation parameters.
             * The generated URLs from this application normally
             * don't contain transformations, but this makes the
             * method safer.
             */
            while (startIndex < parts.length) {

                String part = parts[startIndex];

                if (part.startsWith("v")
                        && part.length() > 1
                        && isNumeric(
                        part.substring(1)
                )) {
                    break;
                }

                startIndex++;
            }

            /*
             * If there is no version segment, Cloudinary
             * public ID starts directly after /upload/.
             */
            if (startIndex >= parts.length) {
                startIndex = 0;
            } else {
                startIndex++;
            }

            StringBuilder publicId =
                    new StringBuilder();

            for (int i = startIndex;
                 i < parts.length;
                 i++) {

                if (publicId.length() > 0) {
                    publicId.append("/");
                }

                publicId.append(parts[i]);
            }

            String result =
                    publicId.toString();

            /*
             * Remove file extension.
             */
            int extensionIndex =
                    result.lastIndexOf(".");

            if (extensionIndex > -1) {
                result =
                        result.substring(
                                0,
                                extensionIndex
                        );
            }

            return result;

        } catch (Exception e) {

            return null;
        }
    }

    // =========================================================
    // NUMBER CHECK
    // =========================================================

    private boolean isNumeric(
            String value
    ) {

        if (value == null
                || value.isBlank()) {
            return false;
        }

        for (char c : value.toCharArray()) {

            if (!Character.isDigit(c)) {
                return false;
            }
        }

        return true;
    }
}