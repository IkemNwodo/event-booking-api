package com.digicore.eventbooking.controller;

import com.digicore.eventbooking.dto.BookingRequest;
import com.digicore.eventbooking.model.Booking;
import com.digicore.eventbooking.model.Event;
import com.digicore.eventbooking.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Tag(name = "Event Controller", description = "Endpoints for managing events and bookings")
public class EventController {

    private final EventService eventService;

    @PostMapping
    @Operation(summary = "Create a new event", description = "Creates a new event with a limited seat capacity. Date must be in the future.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Event created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body or validation failed")
    })
    public ResponseEntity<Event> createEvent(@Valid @RequestBody Event event) {
        Event createdEvent = eventService.createEvent(event);
        return new ResponseEntity<>(createdEvent, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "List all events", description = "Retrieves all events using pagination.")
    @ApiResponse(responseCode = "200", description = "List of events retrieved successfully")
    public ResponseEntity<Page<Event>> listEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Event> events = eventService.listAllEvents(pageable);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get event by ID", description = "Retrieves details of a specific event by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Event found"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<Event> getEventById(@PathVariable Long id) {
        Event event = eventService.getEventById(id);
        return ResponseEntity.ok(event);
    }

    @PostMapping("/{id}/bookings")
    @Operation(summary = "Book a seat at an event", description = "Books a seat for an attendee at a specific event. Validates event capacity, status, and duplicate booking by email.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Seat booked successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed or business rule violation (closed event, full capacity, or duplicate email)"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<Booking> bookSeat(
            @PathVariable Long id,
            @Valid @RequestBody BookingRequest bookingRequest) {
        Booking booking = Booking.builder()
                .attendeeName(bookingRequest.getAttendeeName())
                .attendeeEmail(bookingRequest.getAttendeeEmail())
                .build();
        Booking createdBooking = eventService.bookSeat(id, booking);
        return new ResponseEntity<>(createdBooking, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/bookings")
    @Operation(summary = "List all bookings for an event", description = "Retrieves all bookings made for a specific event using pagination.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of bookings retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<Page<Booking>> listBookingsForEvent(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Booking> bookings = eventService.listBookingsForEvent(id, pageable);
        return ResponseEntity.ok(bookings);
    }
}
