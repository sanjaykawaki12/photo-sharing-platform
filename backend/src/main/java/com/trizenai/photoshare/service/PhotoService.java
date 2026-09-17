package com.trizenai.photoshare.service;

import com.trizenai.photoshare.dto.PhotoDtos.*;
import com.trizenai.photoshare.exception.ApiExceptions.*;
import com.trizenai.photoshare.model.Event;
import com.trizenai.photoshare.model.Photo;
import com.trizenai.photoshare.model.Role;
import com.trizenai.photoshare.model.User;
import com.trizenai.photoshare.repository.PhotoRepository;
import com.trizenai.photoshare.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class PhotoService {

    private static final List<String> ALLOWED_CONTENT_TYPES =
            List.of(
                    "image/jpeg",
                    "image/png",
                    "image/webp",
                    "image/heic",
                    "image/heif"
            );

    private static final long MAX_FILE_SIZE = 25L * 1024 * 1024; // 25 MB

    private final PhotoRepository photoRepository;
    private final UserRepository userRepository;
    private final EventService eventService;
    private final StorageService storageService;

    public PhotoService(
            PhotoRepository photoRepository,
            UserRepository userRepository,
            EventService eventService,
            StorageService storageService
    ) {
        this.photoRepository = photoRepository;
        this.userRepository = userRepository;
        this.eventService = eventService;
        this.storageService = storageService;
    }

    // =========================================================
    // UPLOAD PHOTOS
    // =========================================================

    public List<PhotoResponse> uploadPhotos(
            User uploader,
            Long eventId,
            List<MultipartFile> files
    ) {

        // Admin or assigned Team Member only
        Event event = eventService.getAccessibleEvent(uploader, eventId);

        if (files == null || files.isEmpty()) {
            throw new BadRequestException(
                    "No files were provided for upload"
            );
        }

        return files.stream().map(file -> {

            validateFile(file);

            try {

                // Store image in configured storage
                // (Cloudinary / S3 / Local)
                String storageKey = storageService.store(
                        file,
                        String.valueOf(event.getId())
                );

                Photo photo = Photo.builder()
                        .eventId(event.getId())
                        .uploadedBy(uploader.getId())
                        .filename(file.getOriginalFilename())
                        .storageLocation(storageKey)
                        .fileSize(file.getSize())
                        .contentType(file.getContentType())
                        .build();

                photo = photoRepository.save(photo);

                return toResponse(photo);

            } catch (IOException ex) {

                throw new BadRequestException(
                        "Failed to upload file: "
                                + file.getOriginalFilename()
                );
            }

        }).toList();
    }

    // =========================================================
    // LIST EVENT PHOTOS
    // =========================================================

    public List<PhotoResponse> listEventPhotos(
            User user,
            Long eventId
    ) {

        Event event = eventService.getAccessibleEvent(
                user,
                eventId
        );

        // ADMIN -> sees all photos
        // TEAM_MEMBER -> sees only own uploaded photos
        List<Photo> photos =
                user.getRole() == Role.ADMIN
                        ? photoRepository.findByEventId(event.getId())
                        : photoRepository.findByEventIdAndUploadedBy(
                        event.getId(),
                        user.getId()
                );

        return photos.stream()
                .map(this::toResponse)
                .toList();
    }

    // =========================================================
    // SELECT / UNSELECT PHOTOS FOR GALLERY
    // =========================================================

    public void setSelection(
            User admin,
            Long eventId,
            SelectPhotosRequest request
    ) {

        // Admin-only
        Event event = eventService.getOwnedEvent(
                admin,
                eventId
        );

        List<Photo> photos =
                photoRepository.findAllById(
                        request.photoIds()
                );

        for (Photo photo : photos) {

            // Security check:
            // Photo must belong to the selected event
            if (!photo.getEventId().equals(event.getId())) {

                throw new BadRequestException(
                        "Photo "
                                + photo.getId()
                                + " does not belong to this event"
                );
            }

            photo.setSelectedForGallery(
                    request.selected()
            );
        }

        photoRepository.saveAll(photos);
    }

    // =========================================================
    // DELETE PHOTO
    // =========================================================

    public void deletePhoto(
            User user,
            Long photoId
    ) {

        // Find photo
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Photo not found: " + photoId
                        )
                );

        // Make sure the user has access to this event
        Event event = eventService.getAccessibleEvent(
                user,
                photo.getEventId()
        );

        // Security:
        // ADMIN can delete any photo in accessible event.
        // TEAM_MEMBER can delete ONLY their own photo.
        boolean allowed =
                user.getRole() == Role.ADMIN
                        || photo.getUploadedBy()
                        .equals(user.getId());

        if (!allowed) {

            throw new ForbiddenException(
                    "You can only delete your own photos"
            );
        }

        // Delete actual image from storage first
        try {

            storageService.delete(
                    photo.getStorageLocation()
            );

        } catch (IOException ex) {

            throw new BadRequestException(
                    "Failed to delete photo from storage"
            );
        }

        // Delete metadata from database
        photoRepository.delete(photo);
    }

    // =========================================================
    // FILE VALIDATION
    // =========================================================

    private void validateFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {

            throw new BadRequestException(
                    "Uploaded file is empty"
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {

            throw new BadRequestException(
                    "File exceeds 25MB limit: "
                            + file.getOriginalFilename()
            );
        }

        if (
                file.getContentType() == null
                        || !ALLOWED_CONTENT_TYPES.contains(
                        file.getContentType()
                )
        ) {

            throw new BadRequestException(
                    "Unsupported file type for: "
                            + file.getOriginalFilename()
            );
        }
    }

    // =========================================================
    // RESPONSE
    // =========================================================

    private PhotoResponse toResponse(Photo photo) {

        String uploaderName =
                userRepository.findById(
                                photo.getUploadedBy()
                        )
                        .map(User::getName)
                        .orElse("Unknown");

        return new PhotoResponse(
                photo.getId(),
                photo.getEventId(),
                photo.getUploadedBy(),
                uploaderName,
                photo.getFilename(),
                storageService.resolveUrl(
                        photo.getStorageLocation()
                ),
                photo.getFileSize(),
                photo.getContentType(),
                photo.isSelectedForGallery(),
                photo.getCreatedAt()
        );
    }
}