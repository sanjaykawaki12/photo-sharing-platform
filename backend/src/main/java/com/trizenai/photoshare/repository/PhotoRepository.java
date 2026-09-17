package com.trizenai.photoshare.repository;

import com.trizenai.photoshare.model.Photo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PhotoRepository extends JpaRepository<Photo, Long> {
    List<Photo> findByEventId(Long eventId);
    List<Photo> findByEventIdAndUploadedBy(Long eventId, Long uploadedBy);
    List<Photo> findByEventIdAndSelectedForGalleryTrue(Long eventId);
}
