package com.trizenai.photoshare.repository;

import com.trizenai.photoshare.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByAdminId(Long adminId);
}
