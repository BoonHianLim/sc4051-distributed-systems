// Problem with Facility regarding bookings
// Redo

package com.example;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class Facility {
    private String facilityName;
    private List<String> bookedPeriod;
    private Map<String, Booking> bookings; 

    public Facility (String facilityName, List<String> bookedPeriod)
    {
        this.facilityName = facilityName;
        this.bookedPeriod = bookedPeriod;
    }

    public Boolean checkAvailability (int startDay, int startHour, int startMinute, int duration)
    {

        int requestedStart = startDay * 24 * 60 + startHour * 60 + startMinute;
        int requestedEnd = requestedStart + duration;

        for (String period : bookedPeriod)
        {
            TimeSlotDecoder timeSlot = new TimeSlotDecoder(period);
            int bookedStartDay = TimeSlotDecoder.DAY_TO_INDEX.get((String) timeSlot.getDecodedTimeSlot().get(0));
            int bookedStartHour = (int) timeSlot.getDecodedTimeSlot().get(1);
            int bookedStartMin = (int) timeSlot.getDecodedTimeSlot().get(2);

            int bookedEndDay = TimeSlotDecoder.DAY_TO_INDEX.get((String) timeSlot.getDecodedTimeSlot().get(3));
            int bookedEndHour = (int) timeSlot.getDecodedTimeSlot().get(4);
            int bookedEndMin = (int) timeSlot.getDecodedTimeSlot().get(5);

            int bookedDuration = (int) timeSlot.getDecodedTimeSlot().get(6);

            int bookedStart = bookedStartDay * 24 * 60 + bookedStartHour * 60 + bookedStartMin;
            int bookedEnd = bookedEndDay * 24 * 60 + bookedEndHour * 60 + bookedEndMin;

            if (requestedStart == bookedStart) {
                return false;
            }

            if (requestedEnd > bookedStart && requestedStart < bookedEnd) {
                return false;
                // user's end > booked's start
                // and user's start < booked's end
            }

        }
        return true;
    }

    public List <String> getAvailableSlots (String day)
    {
        List<int[]> bookedRanges = new ArrayList<>();
        for (String period: bookedPeriod)
        {
            TimeSlotDecoder timeSlot = new TimeSlotDecoder(period);
            String bookedStartDay = (String) timeSlot.getDecodedTimeSlot().get(0);
            String bookedEndDay = (String) timeSlot.getDecodedTimeSlot().get(3);

            if (bookedStartDay == day)
            {
                int start = (int) timeSlot.getDecodedTimeSlot().get(1) * 60 + (int) timeSlot.getDecodedTimeSlot().get(2);
                int end = (int) timeSlot.getDecodedTimeSlot().get(4) * 60 + (int) timeSlot.getDecodedTimeSlot().get(5);
                bookedRanges.add(new int[]{start, end});
            }
        }

        bookedRanges.sort((a,b) -> Integer.compare(a[0], b[0]));

        List<String> availableSlots = new ArrayList<>();
        int current = 0;

        for (int[] range : bookedRanges)
        {
            if (current < range[0])
            {
                availableSlots.add(formatTime(current) + " - " + formatTime(range[0]));
            }
            current = Math.max(current, range[1]);
        }

        if (current < 1440)
        {
            availableSlots.add(formatTime(current ) + " - " + formatTime(1440));
        }
        return availableSlots;

    }

    private String formatTime(int totalMinutes) {
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        return String.format("%02d:%02d", hours, minutes);
    }

    public void updateAvailability (int startDay, int startHour, int endDay, int endHour, int endMinute, boolean isBooked)
    {
        // save and remove current availability
        // checkAvailbility function
            // if available, add new availability
            // if not available, re add the current availability
    }
}
