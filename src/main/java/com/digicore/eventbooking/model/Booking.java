package com.digicore.eventbooking.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "bookings",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"event_id", "attendee_email"})
    },
    indexes = {
        @Index(name = "idx_booking_event", columnList = "event_id"),
        @Index(name = "idx_booking_event_email", columnList = "event_id, attendee_email")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Event ID is required")
    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @NotBlank(message = "Attendee name is required")
    @Size(max = 100, message = "Attendee name cannot exceed 100 characters")
    @Column(name = "attendee_name", nullable = false, length = 100)
    private String attendeeName;

    @NotBlank(message = "Attendee email is required")
    @Email(message = "Attendee email must be a valid email address")
    @Size(max = 100, message = "Attendee email cannot exceed 100 characters")
    @Column(name = "attendee_email", nullable = false, length = 100)
    private String attendeeEmail;

    @Column(name = "booked_at", nullable = false)
    private LocalDateTime bookedAt;

    @PrePersist
    protected void onCreate() {
        if (this.bookedAt == null) {
            this.bookedAt = LocalDateTime.now();
        }
    }
}
