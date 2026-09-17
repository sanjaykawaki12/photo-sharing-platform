package com.trizenai.photoshare.controller;

import com.trizenai.photoshare.config.CurrentUserProvider;
import com.trizenai.photoshare.dto.GalleryDtos.*;
import com.trizenai.photoshare.model.User;
import com.trizenai.photoshare.service.GalleryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
public class GalleryController {

    private final GalleryService galleryService;
    private final CurrentUserProvider currentUserProvider;

    public GalleryController(GalleryService galleryService, CurrentUserProvider currentUserProvider) {
        this.galleryService = galleryService;
        this.currentUserProvider = currentUserProvider;
    }

    /** Admin-only: select photos beforehand via PhotoController, then publish here. */
    @PostMapping("/api/events/{eventId}/gallery/publish")
    public ResponseEntity<PublishGalleryResponse> publish(@PathVariable Long eventId,
                                                            @RequestBody(required = false) PublishGalleryRequest request) {
        User admin = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(galleryService.publish(admin, eventId, request));
    }

    /** Public endpoint: customer supplies the link code + PIN, no login required. */
    @PostMapping("/api/gallery/{linkCode}/access")
    public ResponseEntity<GalleryViewResponse> access(@PathVariable String linkCode,
                                                        @Valid @RequestBody AccessGalleryRequest request) {
        return ResponseEntity.ok(galleryService.viewGallery(linkCode, request));
    }
    @GetMapping("/gallery/{linkCode}")
    public String openGallery(@PathVariable String linkCode) {
        return "forward:/gallery.html?code=" + linkCode;
    }
}
