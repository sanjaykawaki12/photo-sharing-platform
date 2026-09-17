package com.trizenai.photoshare.controller;

import com.trizenai.photoshare.config.CurrentUserProvider;
import com.trizenai.photoshare.dto.PhotoDtos.*;
import com.trizenai.photoshare.model.User;
import com.trizenai.photoshare.service.PhotoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PhotoController {

    private final PhotoService photoService;
    private final CurrentUserProvider currentUserProvider;

    public PhotoController(
            PhotoService photoService,
            CurrentUserProvider currentUserProvider
    ) {
        this.photoService = photoService;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Team Member or Admin uploads one or more photos
     * to an event they have access to.
     */
    @PostMapping(
            value = "/events/{eventId}/photos",
            consumes = "multipart/form-data"
    )
    public ResponseEntity<List<PhotoResponse>> upload(
            @PathVariable Long eventId,
            @RequestParam("files") List<MultipartFile> files
    ) {

        User user = currentUserProvider.getCurrentUser();

        return ResponseEntity.ok(
                photoService.uploadPhotos(user, eventId, files)
        );
    }

    /**
     * List photos uploaded to an event.
     */
    @GetMapping("/events/{eventId}/photos")
    public ResponseEntity<List<PhotoResponse>> list(
            @PathVariable Long eventId
    ) {

        User user = currentUserProvider.getCurrentUser();

        return ResponseEntity.ok(
                photoService.listEventPhotos(user, eventId)
        );
    }

    /**
     * Admin-only:
     * Mark or unmark photos for the customer gallery.
     */
    @PatchMapping("/events/{eventId}/photos/selection")
    public ResponseEntity<Void> select(
            @PathVariable Long eventId,
            @RequestBody SelectPhotosRequest request
    ) {

        User admin = currentUserProvider.getCurrentUser();

        photoService.setSelection(
                admin,
                eventId,
                request
        );

        return ResponseEntity.ok().build();
    }

    /**
     * Delete a photo.
     *
     * Authorization is handled inside PhotoService:
     * - ADMIN can delete photos
     * - TEAM_MEMBER can delete only their own photos
     * - TEAM_MEMBER cannot delete another member's photo
     */
    @DeleteMapping("/photos/{photoId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long photoId
    ) {

        User user = currentUserProvider.getCurrentUser();

        photoService.deletePhoto(
                user,
                photoId
        );

        return ResponseEntity.noContent().build();
    }
}