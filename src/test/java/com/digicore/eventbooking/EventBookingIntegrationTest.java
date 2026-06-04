package com.digicore.eventbooking;

import com.digicore.eventbooking.model.Booking;
import com.digicore.eventbooking.model.Event;
import com.digicore.eventbooking.model.EventStatus;
import com.digicore.eventbooking.repository.BookingRepository;
import com.digicore.eventbooking.repository.EventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EventBookingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        bookingRepository.deleteAll();
        eventRepository.deleteAll();
    }

    @Test
    void testCreateEventAndListEvents() throws Exception {
        Event event = Event.builder()
                .title("Tech Conference 2026")
                .description("A great tech meetup")
                .date(LocalDateTime.now().plusMonths(3))
                .venue("Paris Hall")
                .totalSeats(100)
                .build();

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title", is("Tech Conference 2026")))
                .andExpect(jsonPath("$.status", is("OPEN")));

        mockMvc.perform(get("/events")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title", is("Tech Conference 2026")));
    }

    @Test
    void testCreateEventValidationError() throws Exception {
        // Event date is in the past
        Event event = Event.builder()
                .title("") // blank title
                .description("A great tech meetup")
                .date(LocalDateTime.now().minusDays(1)) // past date
                .venue("Paris Hall")
                .totalSeats(0) // invalid total seats
                .build();

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.details.title").exists())
                .andExpect(jsonPath("$.details.date").exists())
                .andExpect(jsonPath("$.details.totalSeats").exists());
    }

    @Test
    void testGetEventNotFound() throws Exception {
        mockMvc.perform(get("/events/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Event not found with ID: 999")));
    }

    @Test
    void testBookSeatAndConcurrences() throws Exception {
        // 1. Create event
        Event event = eventRepository.save(Event.builder()
                .title("Limited Event")
                .description("Small event")
                .date(LocalDateTime.now().plusDays(5))
                .venue("Room A")
                .totalSeats(2)
                .bookedSeats(0)
                .status(EventStatus.OPEN)
                .build());

        // 2. Book seat 1
        Map<String, String> bookingReq1 = new HashMap<>();
        bookingReq1.put("attendeeName", "John Doe");
        bookingReq1.put("attendeeEmail", "john@gmail.com");

        mockMvc.perform(post("/events/" + event.getId() + "/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingReq1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.attendeeName", is("John Doe")));

        // Verify seats incremented
        Event updatedEvent = eventRepository.findById(event.getId()).orElseThrow();
        assertEquals(1, updatedEvent.getBookedSeats());
        assertEquals(EventStatus.OPEN, updatedEvent.getStatus());

        // 3. Book seat 2 (different attendee) -> capacity reached
        Map<String, String> bookingReq2 = new HashMap<>();
        bookingReq2.put("attendeeName", "Jane Doe");
        bookingReq2.put("attendeeEmail", "jane@gmail.com");

        mockMvc.perform(post("/events/" + event.getId() + "/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingReq2)))
                .andExpect(status().isCreated());

        // Verify seats full, auto-closed
        updatedEvent = eventRepository.findById(event.getId()).orElseThrow();
        assertEquals(2, updatedEvent.getBookedSeats());
        assertEquals(EventStatus.CLOSED, updatedEvent.getStatus());

        // 4. Try to book seat 3 -> should fail
        Map<String, String> bookingReq3 = new HashMap<>();
        bookingReq3.put("attendeeName", "Bob Miller");
        bookingReq3.put("attendeeEmail", "bob@gmail.com");

        mockMvc.perform(post("/events/" + event.getId() + "/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingReq3)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Cannot book seat. Event status is CLOSED.")));
    }

    @Test
    void testDuplicateBookingFails() throws Exception {
        Event event = eventRepository.save(Event.builder()
                .title("Duplicate Test Event")
                .description("Desc")
                .date(LocalDateTime.now().plusDays(5))
                .venue("Room B")
                .totalSeats(10)
                .bookedSeats(0)
                .status(EventStatus.OPEN)
                .build());

        Map<String, String> bookingReq = new HashMap<>();
        bookingReq.put("attendeeName", "John Digi");
        bookingReq.put("attendeeEmail", "john@gmail.com");

        // First booking
        mockMvc.perform(post("/events/" + event.getId() + "/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingReq)))
                .andExpect(status().isCreated());

        // Second booking with same email
        mockMvc.perform(post("/events/" + event.getId() + "/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("has already booked a seat")));
    }

    @Test
    void testCancelBookingAndReopenEvent() throws Exception {
        // Create an event of size 1 and book it
        Event event = eventRepository.save(Event.builder()
                .title("Micro Event")
                .description("1 seat only")
                .date(LocalDateTime.now().plusDays(5))
                .venue("Micro Room")
                .totalSeats(1)
                .bookedSeats(0)
                .status(EventStatus.OPEN)
                .build());

        Booking booking = bookingRepository.save(Booking.builder()
                .eventId(event.getId())
                .attendeeName("Solo Traveler")
                .attendeeEmail("solo@gmail.com")
                .bookedAt(LocalDateTime.now())
                .build());

        event.setBookedSeats(1);
        event.setStatus(EventStatus.CLOSED);
        eventRepository.save(event);

        // Cancel the booking
        mockMvc.perform(delete("/bookings/" + booking.getId()))
                .andExpect(status().isOk());

        // Verify booking deleted and event reopened
        assertTrue(bookingRepository.findById(booking.getId()).isEmpty());
        Event reopenedEvent = eventRepository.findById(event.getId()).orElseThrow();
        assertEquals(0, reopenedEvent.getBookedSeats());
        assertEquals(EventStatus.OPEN, reopenedEvent.getStatus());
    }
}
