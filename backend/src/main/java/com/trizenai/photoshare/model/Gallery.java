package com.trizenai.photoshare.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "gallery", uniqueConstraints = @UniqueConstraint(columnNames = "link_code"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gallery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    /** Random URL-safe code, e.g. "abc123" used in /gallery/{linkCode} */
    @Column(name = "link_code", nullable = false, unique = true)
    private String linkCode;

    /** Hashed PIN - never stored/returned in plain text after creation response. */
    @Column(name = "pin_hash", nullable = false)
    private String pinHash;

    @Builder.Default
    @Column(nullable = false)
    private boolean published = true;

    @Builder.Default
    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    /** Optional expiry (bonus feature hook). Null = never expires. */
    private Instant expiresAt;
}
