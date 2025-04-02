package com.example;

import java.net.InetAddress;
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
     * @return the confirmation ID or an error message
     */
    public String bookFacility(String facilityName, String timeSlot) {
        // Validate input
        if (facilityName == null || facilityName.isEmpty()) {
            return "Error: Facility name cannot be empty";
        }
        if (timeSlot == null || timeSlot.isEmpty()) {
            return "Error: Time slot cannot be empty";
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

        TimeSlotDecoder timeSlotDecoder = new TimeSlotDecoder(timeSlot);
        if (!timeSlotDecoder.getStartDay().equals(timeSlotDecoder.getEndDay())) {
            return "Error: Facility is not available for more than one day";
        }
        if (timeSlotDecoder.getStartHour() < 8) {
            return "Error: Facility is not available before 8 AM";
        }
        if (timeSlotDecoder.getEndHour() == 20 && timeSlotDecoder.getEndMin() > 0 || timeSlotDecoder.getEndHour() >= 21) {
            return "Error: Facility is not available after 8 PM";
        }
        if (!timeSlotDecoder.endAfterStart()) {
            return "Error: End time must be after start time";
        }

        try {
            // Create a new booking
            Booking booking = new Booking(facilityName, timeSlot);
            
            // Add the booking to the facility
            boolean added = facility.addBooking(booking);
            
            if (added) {
                // Return the confirmation ID
                return booking.getConfirmationID();
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
            return true;
        }

        // Find the booking by confirmation ID
        Booking booking = findBookingByConfirmationId(confirmationId);
        if (booking == null) {
            return true;
        }

        // Find the facility for this booking
        Optional<Facility> facilityOpt = facilities.stream()
                .filter(f -> f.getFacilityName().equals(booking.getFacilityName()))
                .findFirst();

        if (facilityOpt.isEmpty()) {
            return true;
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
                throw new IllegalStateException("New time slot is not available");
            }
            
            // Add the updated booking back to the facility
            boolean added = facility.addBooking(booking);
            
            return added;
        } catch(IllegalStateException | IllegalArgumentException e) {
            // Restore the original time slot
            booking.shiftBooking(-minuteOffset);
            facility.addBooking(booking);
            throw e;
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
            
            // Extend the booking
            booking.extendBooking(additionalMinutes);

            // check the day of the booking
            TimeSlotDecoder timeSlotDecoder = new TimeSlotDecoder(booking.getTimeSlot());

            if (!timeSlotDecoder.getStartDay().equals(timeSlotDecoder.getEndDay())) {
                throw new IllegalStateException("Cannot extend booking: booking spans multiple days");
            }
            if (timeSlotDecoder.getEndHour() == 20 && timeSlotDecoder.getEndMin() > 0 || timeSlotDecoder.getEndHour() >= 21) {
                throw new IllegalStateException("Cannot extend booking: booking ends after 8 PM");
            }
            if (additionalMinutes > 6*24*60) {
                throw new IllegalStateException("Cannot extend booking: maximum extension is 6 days");
            }
            
            // Check if the new time slot is available
            if (!facility.checkAvailability(booking.getTimeSlot())) {
                throw new IllegalStateException("Cannot extend booking: new time slot is not available");
            }
            
            // Add the booking with the extended time slot
            boolean added = facility.addBooking(booking);
            
            return added;
            
        } catch (IllegalStateException e) {
            // Restore the original time slot
            booking.extendBooking(-additionalMinutes);
            facility.addBooking(booking);
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to extend booking: " + e.getMessage(), e);
        }
    }

    public boolean registerClient(String facilityName, int monitorPeriodinMinutes, int port, InetAddress clientAddress) {

        for (Facility facility: facilities) {
            if (facility.getFacilityName().equals(facilityName)) {
                MonitoringClient tempClient = new MonitoringClient(clientAddress, port, monitorPeriodinMinutes, facilityName);
                clients.add(tempClient);
                return true;
            }
        }
        return false;
    }

    public boolean deregisterClient(InetAddress clientAddress, int port) {
        for (MonitoringClient client: clients) {
            if (client.getClientAddress() == clientAddress && client.getPort() == port) {
                clients.remove(client);
                return true;
            }
        }
        return false;
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

    public List<MonitoringClient> getAllClients() {
        return new ArrayList<>(clients);
    }
}