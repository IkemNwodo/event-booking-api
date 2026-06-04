package com.digicore.eventbooking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingRequest {

    @NotBlank(message = "Attendee name is required")
    @Size(max = 100, message = "Attendee name cannot exceed 100 characters")
    private String attendeeName;

    @NotBlank(message = "Attendee email is required")
    @Email(message = "Attendee email must be a valid email address")
    @Size(max = 100, message = "Attendee email cannot exceed 100 characters")
    private String attendeeEmail;
}
