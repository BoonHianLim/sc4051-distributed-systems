package com.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class Facility {
    private UUID facilityID;
    private String facilityName;
    private List<Booking> bookings;
    
    // Default operating hours
    private static final int OPENING_HOUR = 8;  // 8 AM
    private static final int CLOSING_HOUR = 20; // 8 PM
    private static final int TIME_SLOT_DURATION = 60; // minutes

    public Facility(String facilityName) {
        this.facilityID = UUID.randomUUID();
        this.facilityName = facilityName;
        this.bookings = new ArrayList<>();
    }

    /**
     * Checks if a facility is available during the specified time period.
     * 
     * @param timePeriod the time period in format "Day,Hour,Minute - Day,Hour,Minute"
     * @return true if the facility is available during the entire time period
     */
    public boolean checkAvailability(String timePeriod) {
        // Decode the requested time period
        TimeSlotDecoder requestedSlot = new TimeSlotDecoder(timePeriod);
        
        // Check if there are any overlapping bookings
        for (Booking booking : bookings) {
            if (isOverlapping(booking.getTimeSlotDecoder(), requestedSlot)) {
                return false; // Found an overlapping booking, not available
            }
        }
        
        return true; // No overlapping bookings found, available
    }

    /**
     * Gets a list of time slots that are available (not booked) for the specified days.
     * 
     * @param days a comma-separated list of days (e.g., "Mon, Tue")
     * @return a list of Booking objects representing available time slots on the specified days
     */
    public List<Booking> getAvailableSlots(String days) {
        // Parse the requested days
        Set<String> requestedDays = parseRequestedDays(days);
        
        // Create a list to store the available time slots
        List<Booking> availableSlots = new ArrayList<>();
        
        // For each requested day, generate the default time slots
        for (String day : requestedDays) {
            // Generate time slots for this day based on facility operating hours
            List<String> dayTimeSlots = generateTimeSlotsForDay(day);
            
            // For each potential time slot, check if it's available
            for (String timeSlot : dayTimeSlots) {
                // If the time slot doesn't overlap with any existing booking, it's available
                if (checkAvailability(timeSlot)) {
                    // Create a temporary booking object to represent this available slot
                    // This booking is not added to the facility's bookings list
                    Booking availableSlot = new Booking(facilityName, timeSlot);
                    availableSlots.add(availableSlot);
                }
            }
        }
        
        return availableSlots;
    }
    
    /**
     * Generates time slots for a specific day based on facility operating hours.
     * 
     * @param day the three-letter day abbreviation (e.g., "Mon")
     * @return a list of time slot strings
     */
    private List<String> generateTimeSlotsForDay(String day) {
        List<String> timeSlots = new ArrayList<>();
        
        // Generate time slots from opening to closing time
        for (int hour = OPENING_HOUR; hour < CLOSING_HOUR; hour++) {
            // For each hour, create a time slot
            int startHour = hour;
            int startMinute = 0;
            
            int endHour = hour;
            int endMinute = TIME_SLOT_DURATION;
            
            // Adjust for slots that cross hour boundaries
            if (endMinute >= 60) {
                endHour += endMinute / 60;
                endMinute = endMinute % 60;
            }
            
            // Create the time slot string
            String timeSlot = String.format("%s,%d,%d - %s,%d,%d", 
                    day, startHour, startMinute, 
                    day, endHour, endMinute);
            
            timeSlots.add(timeSlot);
        }
        
        return timeSlots;
    }

    /**
     * Updates the availability of the facility for a specific time period.
     * 
     * @param timePeriod the time period in format "Day,Hour,Minute - Day,Hour,Minute"
     * @param isBooked true to add a booking, false to remove a booking
     */
    public void updateAvailability(String timePeriod, boolean isBooked) {
        if (isBooked) {
            // Check if the time period is available
            if (checkAvailability(timePeriod)) {
                // Add a new booking for this time period
                Booking newBooking = new Booking(facilityName, timePeriod);
                bookings.add(newBooking);
            } else {
                throw new IllegalStateException("Cannot book unavailable time period: " + timePeriod);
            }
        } else {
            // Remove any bookings that match this time period
            bookings.removeIf(booking -> booking.getTimeSlot().equals(timePeriod));
        }
    }
    
    /**
     * Cancels a booking by its confirmation ID.
     * 
     * @param confirmationID the confirmation ID of the booking to cancel
     * @return true if the booking was found and canceled, false otherwise
     */
    public boolean cancelBooking(String confirmationID) {
        if (confirmationID == null || confirmationID.isEmpty()) {
            throw new IllegalArgumentException("Confirmation ID cannot be null or empty");
        }
        
        // Find the booking with the matching confirmation ID
        int bookingIndex = -1;
        for (int i = 0; i < bookings.size(); i++) {
            if (bookings.get(i).getConfirmationID().equals(confirmationID)) {
                bookingIndex = i;
                break;
            }
        }
        
        // If booking found, remove it and return true
        if (bookingIndex >= 0) {
            bookings.remove(bookingIndex);
            return true;
        }
        
        // Booking not found
        return false;
    }
    
    /**
     * Overloaded method to cancel a booking by passing the booking object.
     * 
     * @param booking the booking to cancel
     * @return true if the booking was found and canceled, false otherwise
     */
    public boolean cancelBooking(Booking booking) {
        if (booking == null) {
            throw new IllegalArgumentException("Booking cannot be null");
        }
        
        return cancelBooking(booking.getConfirmationID());
    }

    /**
     * Adds a booking to the facility.
     * 
     * @param booking the booking to add
     * @return true if the booking was added successfully
     * @throws IllegalStateException if the booking overlaps with an existing booking
     */
    public boolean addBooking(Booking booking) {
        // Verify that the booking is for this facility
        if (!booking.getFacilityName().equals(facilityName)) {
            throw new IllegalArgumentException("Booking facility name doesn't match this facility");
        }
        
        // Check availability
        if (!checkAvailability(booking.getTimeSlot())) {
            throw new IllegalStateException("Time slot is not available for booking");
        }
        
        // Add the booking
        return bookings.add(booking);
    }

    /**
     * Gets all bookings for this facility.
     * 
     * @return a list of all bookings
     */
    public List<Booking> getBookings() {
        return new ArrayList<>(bookings);
    }

    /**
     * Gets the facility ID.
     * 
     * @return the facility ID
     */
    public UUID getFacilityID() {
        return facilityID;
    }

    /**
     * Gets the facility name.
     * 
     * @return the facility name
     */
    public String getFacilityName() {
        return facilityName;
    }

    /* Helper methods */

    /**
     * Checks if two time slots overlap.
     * 
     * @param slot1 the first time slot
     * @param slot2 the second time slot
     * @return true if the time slots overlap
     */
    private boolean isOverlapping(TimeSlotDecoder slot1, TimeSlotDecoder slot2) {
        // Convert to day indices and minutes
        int slot1Start = TimeSlotDecoder.DAY_TO_INDEX.get(slot1.getStartDay()) * 24 * 60 
                        + slot1.getStartHour() * 60 + slot1.getStartMin();
        int slot1End = slot1Start + slot1.getDurationMinutes();
        
        int slot2Start = TimeSlotDecoder.DAY_TO_INDEX.get(slot2.getStartDay()) * 24 * 60 
                        + slot2.getStartHour() * 60 + slot2.getStartMin();
        int slot2End = slot2Start + slot2.getDurationMinutes();
        
        // Check for overlap
        return (slot1Start < slot2End && slot2Start < slot1End);
    }

    /**
     * Parses a comma-separated list of days.
     * 
     * @param days the days string (e.g., "Mon, Tue")
     * @return a set of day abbreviations
     */
    private Set<String> parseRequestedDays(String days) {
        return Arrays.stream(days.split(","))
                    .map(String::trim)
                    .collect(Collectors.toSet());
    }
}