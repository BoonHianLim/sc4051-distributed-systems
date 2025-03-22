package com.example;

import java.util.*;

public class BookingService {
    private List<Facility> facilities;
    private List<MonitoringClient> clients;

    /**
     * Constructs a new BookingService instance
     */
    public BookingService() {
        this.facilities = new ArrayList<>();
        this.clients = new ArrayList<>();
    }

    /**
     * Adds a facility to the service
     * 
     * @param facility the facility to add
     * @return true if the facility was added successfully
     */
    public boolean addFacility(Facility facility) {
        if (facility == null) {
            throw new IllegalArgumentException("Facility cannot be null");
        }
        return facilities.add(facility);
    }

    /**
     * Lists availability for a facility for the given days
     * 
     * @param facilityName the name of the facility
     * @param days a comma-separated list of days (e.g., "Mon, Tue")
     * @return a list of available time slots or an error message
     */
    public List<String> listAvailability(String facilityName, String days) {
        // If there is no facilityName, an error message should be displayed
        if (facilityName == null || facilityName.isEmpty()) {
            return List.of("Error: Facility name cannot be empty");
        }
        
        // If days is empty or null
        if (days == null || days.isEmpty()) {
            return List.of("Error: Days specification cannot be empty");
        }

        // Find the facility by name
        Optional<Facility> facilityOpt = facilities.stream()
                .filter(f -> f.getFacilityName().equals(facilityName))
                .findFirst();

        if (facilityOpt.isEmpty()) {
            return List.of("Error: Facility '" + facilityName + "' not found");
        }

        Facility facility = facilityOpt.get();
        List<String> availableSlots = new ArrayList<>();
        
        // Validate the day abbreviations
        String[] daysList = days.split(",");
        for (int i = 0; i < daysList.length; i++) {
            String day = daysList[i].trim();
            if (!TimeSlotDecoder.DAY_TO_INDEX.containsKey(day)) {
                return List.of("Error: Invalid day format. Use three-letter abbreviations (Mon, Tue, etc.)");
            }
        }
        
        // Get available bookings for the specified days
        List<Booking> availableBookings = facility.getAvailableSlots(days);
        
        // If no available slots
        if (availableBookings.isEmpty()) {
            availableSlots.add("No available slots for the specified days: " + days);
        } else {
            for (Booking booking : availableBookings) {
                availableSlots.add(booking.getTimeSlot());
            }
        }
        
        return availableSlots;
    }

    /**
     * Books a facility for a time slot
     * 
     * @param facilityName the name of the facility
     * @param timeSlot the time slot to book
     * @param clientName the name of the client making the booking
     * @return the confirmation ID or an error message
     */
    public String bookFacility(String facilityName, String timeSlot, String clientName) {
        // Validate input
        if (facilityName == null || facilityName.isEmpty()) {
            return "Error: Facility name cannot be empty";
        }
        if (timeSlot == null || timeSlot.isEmpty()) {
            return "Error: Time slot cannot be empty";
        }
        if (clientName == null || clientName.isEmpty()) {
            return "Error: Client name cannot be empty";
        }

        // Find the facility by name
        Optional<Facility> facilityOpt = facilities.stream()
                .filter(f -> f.getFacilityName().equals(facilityName))
                .findFirst();

        if (facilityOpt.isEmpty()) {
            return "Error: Facility '" + facilityName + "' not found";
        }

        Facility facility = facilityOpt.get();

        // Check if the facility is available during the requested time slot
        if (!facility.checkAvailability(timeSlot)) {
            return "Error: Facility is not available during the requested time slot";
        }

        try {
            // Create a new booking
            Booking booking = new Booking(facilityName, timeSlot, clientName);
            
            // Add the booking to the facility
            boolean added = facility.addBooking(booking);
            
            if (added) {
                // Return the confirmation ID
                return "Booking confirmed. Your confirmation ID is: " + booking.getConfirmationID();
            } else {
                return "Error: Failed to add booking";
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            return "Error: " + e.getMessage();
        }
    }
    /**
     * Cancels a booking
     * 
     * @param confirmationId the confirmation ID of the booking to cancel
     * @return true if the booking was canceled successfully, false otherwise
     */
    public boolean cancelBooking(String confirmationId) {
        // Validate input
        if (confirmationId == null || confirmationId.isEmpty()) {
            throw new IllegalArgumentException("Confirmation ID cannot be empty");
        }

        // Find the booking by confirmation ID
        Booking booking = findBookingByConfirmationId(confirmationId);
        if (booking == null) {
            throw new IllegalArgumentException("Booking with confirmation ID '" + confirmationId + "' not found");
        }

        // Find the facility for this booking
        Optional<Facility> facilityOpt = facilities.stream()
                .filter(f -> f.getFacilityName().equals(booking.getFacilityName()))
                .findFirst();

        if (facilityOpt.isEmpty()) {
            throw new IllegalArgumentException("Facility for booking not found");
        }

        Facility facility = facilityOpt.get();

        // Cancel the booking
        boolean canceled = facility.cancelBooking(confirmationId);
        
        return canceled;
    }

     /**
     * Edits a booking by shifting it by the specified number of minutes
     * 
     * @param confirmationId the confirmation ID of the booking to edit
     * @param minuteOffset the number of minutes to shift the booking
     * @return true if the booking was edited successfully, false otherwise
     */
    public boolean editBooking(String confirmationId, int minuteOffset) {
        // Validate input
        if (confirmationId == null || confirmationId.isEmpty()) {
            throw new IllegalArgumentException("Confirmation ID cannot be empty");
        }

        // Find the booking by confirmation ID
        Booking booking = findBookingByConfirmationId(confirmationId);
        if (booking == null) {
            throw new IllegalArgumentException("Booking with confirmation ID '" + confirmationId + "' not found");
        }

        // Find the facility for this booking
        Optional<Facility> facilityOpt = facilities.stream()
                .filter(f -> f.getFacilityName().equals(booking.getFacilityName()))
                .findFirst();

        if (facilityOpt.isEmpty()) {
            throw new IllegalArgumentException("Facility for booking not found");
        }

        Facility facility = facilityOpt.get();

        try {
            // Temporarily remove the booking from the facility
            facility.cancelBooking(booking);
            
            // Shift the booking
            booking.shiftBooking(minuteOffset);
            
            // Check if the new time slot is available
            if (!facility.checkAvailability(booking.getTimeSlot())) {
                // Revert the shift and re-add the original booking
                booking.shiftBooking(-minuteOffset);
                facility.addBooking(booking);
                throw new IllegalStateException("New time slot is not available");
            }
            
            // Add the updated booking back to the facility
            boolean added = facility.addBooking(booking);
            
            return added;
        } catch (Exception e) {
            throw new RuntimeException("Failed to edit booking: " + e.getMessage(), e);
        }
    }
    /**
     * Extends a booking by the specified number of minutes
     * 
     * @param confirmationId the confirmation ID of the booking to extend
     * @param additionalMinutes the number of additional minutes
     * @return true if the booking was extended successfully, false otherwise
     */
    public boolean extendBooking(String confirmationId, int additionalMinutes) {
        if (additionalMinutes <= 0) {
            throw new IllegalArgumentException("Additional minutes must be positive");
        }
        
        // Validate input
        if (confirmationId == null || confirmationId.isEmpty()) {
            throw new IllegalArgumentException("Confirmation ID cannot be empty");
        }

        // Find the booking by confirmation ID
        Booking booking = findBookingByConfirmationId(confirmationId);
        if (booking == null) {
            throw new IllegalArgumentException("Booking with confirmation ID '" + confirmationId + "' not found");
        }

        // Find the facility for this booking
        Optional<Facility> facilityOpt = facilities.stream()
                .filter(f -> f.getFacilityName().equals(booking.getFacilityName()))
                .findFirst();

        if (facilityOpt.isEmpty()) {
            throw new IllegalArgumentException("Facility for booking not found");
        }

        Facility facility = facilityOpt.get();
        
        try {
            // Temporarily remove the booking from the facility
            facility.cancelBooking(booking);
            
            // Calculate the duration of the existing booking
            TimeSlotDecoder decoder = booking.getTimeSlotDecoder();
            int currentDuration = decoder.getDurationMinutes();
            
            // Use shiftBooking to extend the booking
            // We're not actually shifting the start time, but extending the end time
            // This can be achieved by first shifting forward by current duration + additional minutes
            // and then shifting back by current duration
            booking.shiftBooking(currentDuration + additionalMinutes);
            booking.shiftBooking(-currentDuration);
            
            // Check if the new time slot is available
            if (!facility.checkAvailability(booking.getTimeSlot())) {
                // Restore the original time slot
                booking.shiftBooking(currentDuration);
                booking.shiftBooking(-(currentDuration + additionalMinutes));
                
                // Re-add the original booking
                facility.addBooking(booking);
                throw new IllegalStateException("Cannot extend booking: new time slot is not available");
            }
            
            // Add the booking with the extended time slot
            boolean added = facility.addBooking(booking);
            
            return added;
            
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to extend booking: " + e.getMessage(), e);
        }
    }
    
    /**
     * Finds a booking by confirmation ID
     * 
     * @param confirmationId the confirmation ID
     * @return the booking, or null if not found
     */
    private Booking findBookingByConfirmationId(String confirmationId) {
        for (Facility facility : facilities) {
            for (Booking booking : facility.getBookings()) {
                if (booking.getConfirmationID().equals(confirmationId)) {
                    return booking;
                }
            }
        }
        return null;
    }
    
    /**
     * Gets all bookings
     * 
     * @return a list of all bookings
     */
    public List<Booking> getAllBookings() {
        List<Booking> allBookings = new ArrayList<>();
        for (Facility facility : facilities) {
            allBookings.addAll(facility.getBookings());
        }
        return allBookings;
    }
    
    /**
     * Gets all facilities
     * 
     * @return a list of all facilities
     */
    public List<Facility> getAllFacilities() {
        return new ArrayList<>(facilities);
    }
}