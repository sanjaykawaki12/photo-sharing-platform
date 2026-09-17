package com.trizenai.photoshare.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;

public class EventDtos {

    public record CreateEventRequest(@NotBlank String name) {}

    public record AddMemberRequest(@Email @NotBlank String email) {}

    public record EventResponse(Long id, String name, Long adminId, Instant createdAt) {}

    public record EventDetailResponse(
            Long id,
            String name,
            Long adminId,
            Instant createdAt,
            List<MemberInfo> members,
            long totalPhotos,
            long selectedPhotos
    ) {}

    public record MemberInfo(Long userId, String name, String email) {}
}
