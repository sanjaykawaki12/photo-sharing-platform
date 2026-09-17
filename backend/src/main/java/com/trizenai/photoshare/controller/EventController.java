package com.trizenai.photoshare.controller;

import com.trizenai.photoshare.config.CurrentUserProvider;
import com.trizenai.photoshare.dto.EventDtos.*;
import com.trizenai.photoshare.model.Event;
import com.trizenai.photoshare.model.User;
import com.trizenai.photoshare.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;
    private final CurrentUserProvider currentUserProvider;

    public EventController(EventService eventService, CurrentUserProvider currentUserProvider) {
        this.eventService = eventService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest request) {
        User admin = currentUserProvider.getCurrentUser();
        Event event = eventService.createEvent(admin, request);
        return ResponseEntity.ok(toResponse(event));
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> listEvents() {
        User user = currentUserProvider.getCurrentUser();
        List<EventResponse> events = eventService.listVisibleEvents(user).stream()
                .map(this::toResponse).toList();
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventDetailResponse> getEvent(@PathVariable Long eventId) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(eventService.getEventDetail(user, eventId));
    }

    @PostMapping("/{eventId}/members")
    public ResponseEntity<Void> addMember(@PathVariable Long eventId, @Valid @RequestBody AddMemberRequest request) {
        User admin = currentUserProvider.getCurrentUser();
        eventService.addMember(admin, eventId, request);
        return ResponseEntity.ok().build();
    }

    private EventResponse toResponse(Event event) {
        return new EventResponse(event.getId(), event.getName(), event.getAdminId(), event.getCreatedAt());
    }
}
