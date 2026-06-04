package com.digicore.eventbooking.service;

import com.digicore.eventbooking.exception.BusinessRuleException;
import com.digicore.eventbooking.exception.ResourceNotFoundException;
import com.digicore.eventbooking.model.Booking;
import com.digicore.eventbooking.model.Event;
import com.digicore.eventbooking.model.EventStatus;
import com.digicore.eventbooking.repository.BookingRepository;
import com.digicore.eventbooking.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private EventService eventService;

    private Event openEvent;
    private Booking booking;

    @BeforeEach
    void setUp() {
        openEvent = Event.builder()
                .id(1L)
                .title("Tech Conference")
                .description("A cool tech event")
                .date(LocalDateTime.now().plusDays(2))
                .venue("Convention Center")
                .totalSeats(5)
                .bookedSeats(0)
                .status(EventStatus.OPEN)
                .build();

        booking = Booking.builder()
                .id(10L)
                .eventId(1L)
                .attendeeName("Alice Smith")
                .attendeeEmail("alice@gmail.com")
                .build();
    }

    @Test
    void createEvent_Success() {
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event result = eventService.createEvent(openEvent);

        assertNotNull(result);
        assertEquals(EventStatus.OPEN, result.getStatus());
        assertEquals(0, result.getBookedSeats());
        verify(eventRepository, times(1)).save(openEvent);
    }

    @Test
    void createEvent_Fails_PastDate() {
        openEvent.setDate(LocalDateTime.now().minusHours(1));

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, 
                () -> eventService.createEvent(openEvent));
        
        assertEquals("Event date must be in the future at time of creation.", exception.getMessage());
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void createEvent_Fails_ZeroSeats() {
        openEvent.setTotalSeats(0);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, 
                () -> eventService.createEvent(openEvent));
        
        assertEquals("Total seats must be greater than 0.", exception.getMessage());
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void bookSeat_Success() {
        when(eventRepository.findByIdWithLock(1L)).thenReturn(Optional.of(openEvent));
        when(bookingRepository.existsByEventIdAndAttendeeEmail(1L, "alice@gmail.com")).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking savedBooking = eventService.bookSeat(1L, booking);

        assertNotNull(savedBooking);
        assertEquals(1, openEvent.getBookedSeats());
        assertEquals(EventStatus.OPEN, openEvent.getStatus());
        verify(bookingRepository, times(1)).save(booking);
        verify(eventRepository, times(1)).save(openEvent);
    }

    @Test
    void bookSeat_Success_AutoCloses() {
        openEvent.setTotalSeats(1);
        openEvent.setBookedSeats(0);

        when(eventRepository.findByIdWithLock(1L)).thenReturn(Optional.of(openEvent));
        when(bookingRepository.existsByEventIdAndAttendeeEmail(1L, "alice@gmail.com")).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking savedBooking = eventService.bookSeat(1L, booking);

        assertNotNull(savedBooking);
        assertEquals(1, openEvent.getBookedSeats());
        assertEquals(EventStatus.CLOSED, openEvent.getStatus());
    }

    @Test
    void bookSeat_Fails_ClosedEvent() {
        openEvent.setStatus(EventStatus.CLOSED);
        when(eventRepository.findByIdWithLock(1L)).thenReturn(Optional.of(openEvent));

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, 
                () -> eventService.bookSeat(1L, booking));

        assertEquals("Cannot book seat. Event status is CLOSED.", exception.getMessage());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void bookSeat_Fails_FullyBooked() {
        openEvent.setBookedSeats(5);
        when(eventRepository.findByIdWithLock(1L)).thenReturn(Optional.of(openEvent));

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, 
                () -> eventService.bookSeat(1L, booking));

        assertEquals("Cannot book seat. Event is fully booked.", exception.getMessage());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void bookSeat_Fails_DuplicateEmail() {
        when(eventRepository.findByIdWithLock(1L)).thenReturn(Optional.of(openEvent));
        when(bookingRepository.existsByEventIdAndAttendeeEmail(1L, "alice@gmail.com")).thenReturn(true);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, 
                () -> eventService.bookSeat(1L, booking));

        assertTrue(exception.getMessage().contains("has already booked a seat"));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void cancelBooking_Success_ReopensEvent() {
        openEvent.setTotalSeats(1);
        openEvent.setBookedSeats(1);
        openEvent.setStatus(EventStatus.CLOSED);

        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(eventRepository.findByIdWithLock(1L)).thenReturn(Optional.of(openEvent));

        eventService.cancelBooking(10L);

        assertEquals(0, openEvent.getBookedSeats());
        assertEquals(EventStatus.OPEN, openEvent.getStatus());
        verify(bookingRepository, times(1)).delete(booking);
        verify(eventRepository, times(1)).save(openEvent);
    }
}
