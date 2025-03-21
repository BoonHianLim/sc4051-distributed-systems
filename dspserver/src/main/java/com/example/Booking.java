package com.example;

import java.util.List;

public class Booking {
    public String confirmationID;
    public String facilityName;
    public String timeSlot; 
    public String clientName;

    public Booking (String confirmationID, String facilityName, String timeSlot, String clientName)
    {
        this.confirmationID = confirmationID;
        this.facilityName = facilityName;
        this.timeSlot = timeSlot;
        this.clientName = clientName;
    }

    public void shiftBooking(int minuteOffset)
    {
        TimeSlotDecoder timeSlotList = new TimeSlotDecoder(timeSlot);
        int bookedStartDay = TimeSlotDecoder.DAY_TO_INDEX.get((String) timeSlotList.getDecodedTimeSlot().get(0));
        int bookedStartHour = (int) timeSlotList.getDecodedTimeSlot().get(1);
        int bookedStartMin = (int) timeSlotList.getDecodedTimeSlot().get(2);
        int bookedDuration = (int) timeSlotList.getDecodedTimeSlot().get(6);

        int bookedStart = bookedStartDay * 24 * 60 + bookedStartHour * 60 + bookedStartMin;
        int bookedEnd = bookedStart + bookedDuration;

        bookedStart += minuteOffset;
        bookedEnd += minuteOffset;
    
        int startDayIndex = bookedStart / (24 * 60);
        int startRemainder = bookedStart % (24 * 60);
        int startHour = startRemainder / 60;
        int startMinute = startRemainder % 60;
    
        int endDayIndex = bookedEnd / (24 * 60);
        int endRemainder = bookedEnd % (24 * 60);
        int endHour = endRemainder / 60;
        int endMinute = endRemainder % 60;
    
        String[] INDEX_TO_DAY = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        String startDay = INDEX_TO_DAY[startDayIndex];
        String endDay = INDEX_TO_DAY[endDayIndex];
    
        this.timeSlot = String.format("%s,%02d,%02d - %s,%02d,%02d",
            startDay, startHour, startMinute,
            endDay, endHour, endMinute
        );
    }

    public String getTimeSlot()
    {
        return timeSlot;
    }

    public String getConfirmationID()
    {
        return confirmationID;
    }

}
