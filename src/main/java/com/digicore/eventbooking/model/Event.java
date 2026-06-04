package com.digicore.eventbooking.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title cannot exceed 100 characters")
    @Column(nullable = false, length = 100)
    private String title;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    @Column(length = 1000)
    private String description;

    @NotNull(message = "Event date is required")
    @Future(message = "Event date must be in the future")
    @Column(nullable = false)
    private LocalDateTime date;

    @NotBlank(message = "Venue is required")
    @Size(max = 200, message = "Venue cannot exceed 200 characters")
    @Column(nullable = false, length = 200)
    private String venue;

    @NotNull(message = "Total seats is required")
    @Min(value = 1, message = "Total seats must be greater than 0")
    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;

    @Builder.Default
    @Column(name = "booked_seats", nullable = false)
    private Integer bookedSeats = 0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status = EventStatus.OPEN;
}
