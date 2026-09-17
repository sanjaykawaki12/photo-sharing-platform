package com.trizenai.photoshare.service;

import com.trizenai.photoshare.dto.EventDtos.*;
import com.trizenai.photoshare.exception.ApiExceptions.*;
import com.trizenai.photoshare.model.Event;
import com.trizenai.photoshare.model.EventMember;
import com.trizenai.photoshare.model.Role;
import com.trizenai.photoshare.model.User;
import com.trizenai.photoshare.repository.EventMemberRepository;
import com.trizenai.photoshare.repository.EventRepository;
import com.trizenai.photoshare.repository.PhotoRepository;
import com.trizenai.photoshare.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final EventMemberRepository eventMemberRepository;
    private final UserRepository userRepository;
    private final PhotoRepository photoRepository;

    public EventService(EventRepository eventRepository, EventMemberRepository eventMemberRepository,
                         UserRepository userRepository, PhotoRepository photoRepository) {
        this.eventRepository = eventRepository;
        this.eventMemberRepository = eventMemberRepository;
        this.userRepository = userRepository;
        this.photoRepository = photoRepository;
    }

    public Event createEvent(User admin, CreateEventRequest request) {
        requireAdmin(admin);
        Event event = Event.builder().name(request.name()).adminId(admin.getId()).build();
        return eventRepository.save(event);
    }

    public void addMember(User admin, Long eventId, AddMemberRequest request) {
        Event event = getOwnedEvent(admin, eventId);

        User member = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new NotFoundException("No registered user with email: " + request.email()));

        if (member.getRole() != Role.TEAM_MEMBER) {
            throw new BadRequestException("Only users registered as TEAM_MEMBER can be added to an event");
        }
        if (eventMemberRepository.existsByEventIdAndUserId(event.getId(), member.getId())) {
            throw new ConflictException("This user is already a member of the event");
        }

        eventMemberRepository.save(EventMember.builder().eventId(event.getId()).userId(member.getId()).build());
    }

    /** Events visible to the current user: owned (Admin) or assigned (Team Member). */
    public List<Event> listVisibleEvents(User user) {
        if (user.getRole() == Role.ADMIN) {
            return eventRepository.findByAdminId(user.getId());
        }
        List<Long> eventIds = eventMemberRepository.findByUserId(user.getId())
                .stream().map(EventMember::getEventId).toList();
        return eventRepository.findAllById(eventIds);
    }

    public EventDetailResponse getEventDetail(User user, Long eventId) {
        Event event = getAccessibleEvent(user, eventId);

        List<MemberInfo> members = eventMemberRepository.findByEventId(event.getId()).stream()
                .map(em -> userRepository.findById(em.getUserId()).orElse(null))
                .filter(u -> u != null)
                .map(u -> new MemberInfo(u.getId(), u.getName(), u.getEmail()))
                .toList();

        long total = photoRepository.findByEventId(event.getId()).size();
        long selected = photoRepository.findByEventIdAndSelectedForGalleryTrue(event.getId()).size();

        return new EventDetailResponse(event.getId(), event.getName(), event.getAdminId(),
                event.getCreatedAt(), members, total, selected);
    }

    /** Verifies the event exists AND belongs to this admin. Used for admin-only mutations. */
    public Event getOwnedEvent(User admin, Long eventId) {
        requireAdmin(admin);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        if (!event.getAdminId().equals(admin.getId())) {
            throw new ForbiddenException("You do not manage this event");
        }
        return event;
    }

    /**
     * Verifies the event exists AND the current user may view it: the owning
     * Admin, or a Team Member assigned to it. Prevents "user attempting to
     * access another event" scenario called out in the requirements.
     */
    public Event getAccessibleEvent(User user, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        boolean isOwningAdmin = user.getRole() == Role.ADMIN && event.getAdminId().equals(user.getId());
        boolean isAssignedMember = user.getRole() == Role.TEAM_MEMBER
                && eventMemberRepository.existsByEventIdAndUserId(eventId, user.getId());

        if (!isOwningAdmin && !isAssignedMember) {
            throw new ForbiddenException("You do not have access to this event");
        }
        return event;
    }

    private void requireAdmin(User user) {
        if (user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only an Admin/Lead can perform this action");
        }
    }
}
