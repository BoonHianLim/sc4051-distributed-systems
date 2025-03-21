package com.example;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.UUID;

/*
 * Represents a booking for a facility with a specific time slot
 */
public class Booking {
    private UUID confirmationID;
    private String facilityName;
    private String timeSlot;
    private String clientName;
    private TimeSlotDecoder timeSlotDecoder;

    /**
     * Constructs a new Booking with the specified details.
     * 
     * @param facilityName the name of the facility being booked
     * @param timeSlot the time slot in format "Day,Hour,Minute - Day,Hour,Minute"
     * @param clientName the name of the client making the booking
     */
    public Booking(String facilityName, String timeSlot, String clientName) {
        this.confirmationID = UUID.randomUUID();
        this.facilityName = facilityName;
        this.timeSlot = timeSlot;
        this.clientName = clientName;
        this.timeSlotDecoder = new TimeSlotDecoder(timeSlot);
    }
    /**
     * Shifts the booking time by the specified number of minutes.
     * 
     * @param minuteOffset the number of minutes to shift the booking time
     */
    public void shiftBooking(int minuteOffset) {
        // Extract current booking details
        String startDay = timeSlotDecoder.getStartDay();
        int startHour = timeSlotDecoder.getStartHour();
        int startMin = timeSlotDecoder.getStartMin();
        
        String endDay = timeSlotDecoder.getEndDay();
        int endHour = timeSlotDecoder.getEndHour();
        int endMin = timeSlotDecoder.getEndMin();

        // Create reference week starting with Monday
        LocalDateTime baseDateTime = LocalDateTime.of(2025, 1, 6, 0, 0);

        System.out.println("Start Day: " + startDay);
        System.out.println("Start Time: " + startHour + ":" + startMin);

        System.out.println("End Day: " + endDay);
        System.out.println("End Time: " + endHour + ":" + endMin);

        // Calculate the day offsets based on the day names
        int startDayOffset = TimeSlotDecoder.DAY_TO_INDEX.get(startDay);
        int endDayOffset = TimeSlotDecoder.DAY_TO_INDEX.get(endDay);
        
        // Create LocalDateTime objects for start and end times
        LocalDateTime startDateTime = baseDateTime.plusDays(startDayOffset)
                                                 .withHour(startHour)
                                                 .withMinute(startMin);
        
        LocalDateTime endDateTime = baseDateTime.plusDays(endDayOffset)
                                               .withHour(endHour)
                                               .withMinute(endMin);
        
        // Apply the minute offset
        LocalDateTime newStartDateTime = startDateTime.plusMinutes(minuteOffset);
        LocalDateTime newEndDateTime = endDateTime.plusMinutes(minuteOffset);
        
        // Format the new time slot string
        String newTimeSlot = formatTimeSlot(newStartDateTime, newEndDateTime);
        
        // Update the booking
        this.timeSlot = newTimeSlot;
        this.timeSlotDecoder = new TimeSlotDecoder(newTimeSlot);
    }

    /**
     * Formats a time slot string from LocalDateTime objects.
     * 
     * @param start the start date and time
     * @param end the end date and time
     * @return a formatted time slot string in the format "Day,Hour,Minute - Day,Hour,Minute"
     */
    private String formatTimeSlot(LocalDateTime start, LocalDateTime end) {
        // Get three-letter day abbreviations (Mon, Tue, etc.)
        String startDay = getDayAbbreviation(start.getDayOfWeek());
        String endDay = getDayAbbreviation(end.getDayOfWeek());
        
        // Format the time slot string
        return String.format("%s,%d,%d - %s,%d,%d",
                startDay, start.getHour(), start.getMinute(),
                endDay, end.getHour(), end.getMinute());
    }

    /**
     * Gets the three-letter abbreviation for a day of week.
     * 
     * @param day the day of week
     * @return the three-letter abbreviation (Mon, Tue, etc.)
     */
    private String getDayAbbreviation(DayOfWeek day) {
        // Use the first three letters of the day name in English
        return day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                 .substring(0, 3);
    }

    /**
     * Gets the current time slot for this booking.
     * 
     * @return the time slot in format "Day,Hour,Minute - Day,Hour,Minute"
     */
    public String getTimeSlot() {
        return timeSlot;
    }

    /**
     * Gets the confirmation ID for this booking.
     * 
     * @return the confirmation ID as a string
     */
    public String getConfirmationID() {
        return confirmationID.toString();
    }

    /**
     * Gets the facility name.
     * 
     * @return the facility name
     */
    public String getFacilityName() {
        return facilityName;
    }

    /**
     * Gets the client name.
     * 
     * @return the client name
     */
    public String getClientName() {
        return clientName;
    }

    /**
     * Gets the TimeSlotDecoder associated with this booking.
     * 
     * @return the TimeSlotDecoder instance
     */
    public TimeSlotDecoder getTimeSlotDecoder() {
        return timeSlotDecoder;
    }

    /**
     * Returns a string representation of the booking.
     * 
     * @return a string containing the booking details
     */
    @Override
    public String toString() {
        return "Booking{" +
                "confirmationID=" + confirmationID +
                ", facilityName='" + facilityName + '\'' +
                ", timeSlot='" + timeSlot + '\'' +
                ", clientName='" + clientName + '\'' +
                '}';
    }
}