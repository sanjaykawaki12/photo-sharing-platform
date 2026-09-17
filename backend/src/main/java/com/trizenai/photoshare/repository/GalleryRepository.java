package com.trizenai.photoshare.repository;

import com.trizenai.photoshare.model.Gallery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GalleryRepository extends JpaRepository<Gallery, Long> {
    Optional<Gallery> findByLinkCode(String linkCode);
    Optional<Gallery> findByEventId(Long eventId);
}
