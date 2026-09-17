package com.trizenai.photoshare.service;

import com.trizenai.photoshare.dto.GalleryDtos.*;
import com.trizenai.photoshare.dto.PhotoDtos.PhotoResponse;
import com.trizenai.photoshare.exception.ApiExceptions.*;
import com.trizenai.photoshare.model.Event;
import com.trizenai.photoshare.model.Gallery;
import com.trizenai.photoshare.model.Photo;
import com.trizenai.photoshare.model.User;
import com.trizenai.photoshare.repository.EventRepository;
import com.trizenai.photoshare.repository.GalleryRepository;
import com.trizenai.photoshare.repository.PhotoRepository;
import com.trizenai.photoshare.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;

@Service
public class GalleryService {

    private static final String LINK_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final BCryptPasswordEncoder PIN_ENCODER = new BCryptPasswordEncoder();

    private final GalleryRepository galleryRepository;
    private final EventService eventService;
    private final PhotoRepository photoRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final StorageService storageService;
    private final String publicBaseUrl;

    public GalleryService(GalleryRepository galleryRepository, EventService eventService,
                           PhotoRepository photoRepository, UserRepository userRepository,
                           EventRepository eventRepository, StorageService storageService,
                           @Value("${app.public-base-url:http://localhost:8080}") String publicBaseUrl) {
        this.galleryRepository = galleryRepository;
        this.eventService = eventService;
        this.photoRepository = photoRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.storageService = storageService;
        this.publicBaseUrl = publicBaseUrl;
    }

    /** Admin selects photos and publishes the gallery, generating a link + PIN. */
    public PublishGalleryResponse publish(User admin, Long eventId, PublishGalleryRequest request) {
        Event event = eventService.getOwnedEvent(admin, eventId); // enforces Admin-only + ownership

        long selectedCount = photoRepository.findByEventIdAndSelectedForGalleryTrue(event.getId()).size();
        if (selectedCount == 0) {
            throw new BadRequestException("Select at least one photo before publishing the gallery");
        }

        String pin = (request != null && request.pin() != null && !request.pin().isBlank())
                ? request.pin().trim()
                : generatePin();
        if (!pin.matches("\\d{4,8}")) {
            throw new BadRequestException("PIN must be 4-8 digits");
        }

        // One gallery per event: republishing rotates the link + PIN.
        Gallery gallery = galleryRepository.findByEventId(event.getId()).orElseGet(Gallery::new);
        gallery.setEventId(event.getId());
        gallery.setLinkCode(generateLinkCode());
        gallery.setPinHash(PIN_ENCODER.encode(pin));
        gallery.setPublished(true);
        if (gallery.getCreatedAt() == null) gallery.setCreatedAt(Instant.now());

        gallery = galleryRepository.save(gallery);

        return new PublishGalleryResponse(
                gallery.getId(), event.getId(), gallery.getLinkCode(),
                publicBaseUrl + "/gallery/" + gallery.getLinkCode(), pin, gallery.getCreatedAt()
        );
    }

    /** Public, unauthenticated access: link code + PIN only (no account needed). */
    public GalleryViewResponse viewGallery(String linkCode, AccessGalleryRequest request) {
        Gallery gallery = galleryRepository.findByLinkCode(linkCode)
                .orElseThrow(() -> new NotFoundException("Gallery not found"));

        if (!gallery.isPublished()) {
            throw new ForbiddenException("This gallery is not currently published");
        }
        if (gallery.getExpiresAt() != null && gallery.getExpiresAt().isBefore(Instant.now())) {
            throw new ForbiddenException("This gallery link has expired");
        }
        if (!PIN_ENCODER.matches(request.pin(), gallery.getPinHash())) {
            throw new InvalidPinException("Incorrect PIN");
        }

        Event event = eventRepository.findById(gallery.getEventId())
                .orElseThrow(() -> new NotFoundException("Event not found"));

        // Only ever return photos explicitly selected+published - never unpublished/unselected photos.
        List<Photo> photos = photoRepository.findByEventIdAndSelectedForGalleryTrue(event.getId());

        List<PhotoResponse> photoResponses = photos.stream().map(p -> new PhotoResponse(
                p.getId(), p.getEventId(), p.getUploadedBy(),
                userRepository.findById(p.getUploadedBy()).map(User::getName).orElse("Unknown"),
                p.getFilename(), storageService.resolveUrl(p.getStorageLocation()),
                p.getFileSize(), p.getContentType(), p.isSelectedForGallery(), p.getCreatedAt()
        )).toList();

        return new GalleryViewResponse(event.getName(), photoResponses);
    }

    private String generateLinkCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(8);
            for (int i = 0; i < 8; i++) sb.append(LINK_CHARS.charAt(RANDOM.nextInt(LINK_CHARS.length())));
            code = sb.toString();
        } while (galleryRepository.findByLinkCode(code).isPresent());
        return code;
    }

    private String generatePin() {
        int pin = 100000 + RANDOM.nextInt(900000); // 6-digit PIN, e.g. 482917
        return String.valueOf(pin);
    }
}
