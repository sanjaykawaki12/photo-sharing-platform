package com.trizenai.photoshare.repository;

import com.trizenai.photoshare.model.EventMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventMemberRepository extends JpaRepository<EventMember, Long> {
    List<EventMember> findByEventId(Long eventId);
    List<EventMember> findByUserId(Long userId);
    Optional<EventMember> findByEventIdAndUserId(Long eventId, Long userId);
    boolean existsByEventIdAndUserId(Long eventId, Long userId);
}
