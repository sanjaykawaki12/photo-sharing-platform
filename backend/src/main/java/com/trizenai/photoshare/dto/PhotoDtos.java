package com.trizenai.photoshare.dto;

import java.time.Instant;
import java.util.List;

public class PhotoDtos {

    public record PhotoResponse(
            Long id,
            Long eventId,
            Long uploadedBy,
            String uploadedByName,
            String filename,
            String url,
            long fileSize,
            String contentType,
            boolean selectedForGallery,
            Instant createdAt
    ) {}

    public record SelectPhotosRequest(List<Long> photoIds, boolean selected) {}
}
