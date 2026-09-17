package com.trizenai.photoshare.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "photo")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;

    @Column(nullable = false)
    private String filename;

    /** Key/path/URL in object storage (S3 key or local path). Actual bytes are NOT stored in the DB. */
    @Column(name = "storage_location", nullable = false)
    private String storageLocation;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "content_type")
    private String contentType;

    @Builder.Default
    @Column(name = "selected_for_gallery", nullable = false)
    private boolean selectedForGallery = false;

    @Builder.Default
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
