package com.digicore.eventbooking.service;

import com.digicore.eventbooking.exception.BusinessRuleException;
import com.digicore.eventbooking.exception.ResourceNotFoundException;
import com.digicore.eventbooking.model.Booking;
import com.digicore.eventbooking.model.Event;
import com.digicore.eventbooking.model.EventStatus;
import com.digicore.eventbooking.repository.BookingRepository;
import com.digicore.eventbooking.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final BookingRepository bookingRepository;

    @Transactional
    public Event createEvent(Event event) {
        if (event.getDate() != null && event.getDate().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Event date must be in the future at time of creation.");
        }
        if (event.getTotalSeats() == null || event.getTotalSeats() <= 0) {
            throw new BusinessRuleException("Total seats must be greater than 0.");
        }
        
        event.setBookedSeats(0);
        event.setStatus(EventStatus.OPEN);
        return eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public Page<Event> listAllEvents(Pageable pageable) {
        return eventRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Event getEventById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + id));
    }

    @Transactional
    public Booking bookSeat(Long eventId, Booking booking) {
        // Fetch event with pessimistic lock to prevent concurrent seat overbooking
        Event event = eventRepository.findByIdWithLock(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));

        if (event.getStatus() == EventStatus.CLOSED) {
            throw new BusinessRuleException("Cannot book seat. Event status is CLOSED.");
        }

        if (event.getBookedSeats() >= event.getTotalSeats()) {
            throw new BusinessRuleException("Cannot book seat. Event is fully booked.");
        }

        if (bookingRepository.existsByEventIdAndAttendeeEmail(eventId, booking.getAttendeeEmail())) {
            throw new BusinessRuleException("Attendee with email " + booking.getAttendeeEmail() + 
                    " has already booked a seat for this event.");
        }

        // Setup booking details
        booking.setEventId(eventId);
        booking.setBookedAt(LocalDateTime.now());
        Booking savedBooking = bookingRepository.save(booking);

        // Update event seats
        event.setBookedSeats(event.getBookedSeats() + 1);
        if (event.getBookedSeats().equals(event.getTotalSeats())) {
            event.setStatus(EventStatus.CLOSED);
        }
        eventRepository.save(event);

        return savedBooking;
    }

    @Transactional
    public void cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        // Fetch corresponding event with lock to adjust seats safely
        Event event = eventRepository.findByIdWithLock(booking.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event associated with this booking does not exist."));

        // Decrement booked seats
        if (event.getBookedSeats() > 0) {
            event.setBookedSeats(event.getBookedSeats() - 1);
        }

        // If event was closed (due to capacity or manually), reopen it if seats are freed
        if (event.getBookedSeats() < event.getTotalSeats()) {
            event.setStatus(EventStatus.OPEN);
        }

        eventRepository.save(event);
        bookingRepository.delete(booking);
    }

    @Transactional(readOnly = true)
    public Page<Booking> listBookingsForEvent(Long eventId, Pageable pageable) {
        // Verify event exists
        if (!eventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Event not found with ID: " + eventId);
        }
        return bookingRepository.findByEventId(eventId, pageable);
    }
}
