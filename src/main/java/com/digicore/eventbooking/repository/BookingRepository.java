package com.digicore.eventbooking.repository;

import com.digicore.eventbooking.model.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    boolean existsByEventIdAndAttendeeEmail(Long eventId, String attendeeEmail);
    
    Page<Booking> findByEventId(Long eventId, Pageable pageable);
}
