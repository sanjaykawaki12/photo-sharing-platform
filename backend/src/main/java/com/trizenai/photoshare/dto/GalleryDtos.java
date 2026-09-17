package com.trizenai.photoshare.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;

public class GalleryDtos {

    public record PublishGalleryRequest(String pin) {}

    /** Returned once at publish time - includes the plain PIN for the Admin to share. */
    public record PublishGalleryResponse(
            Long galleryId,
            Long eventId,
            String linkCode,
            String galleryUrl,
            String pin,
            Instant createdAt
    ) {}

    public record AccessGalleryRequest(@NotBlank String pin) {}

    public record GalleryViewResponse(
            String eventName,
            List<PhotoDtos.PhotoResponse> photos
    ) {}
}
