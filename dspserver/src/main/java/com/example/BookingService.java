package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BookingService {

        private Map<String, Facility> facilities;
        private Map<String, Booking> bookings;
        private Map<String, MonitoringClient> monitoringClients;

        public BookingService ()
        {

        }

        public List<String> listAvailability (String facilityName, String days)
        {   
            Facility facility = facilities.get(facilityName);

            if (facility == null) {
                System.out.println("Facility not found.");
                return List.of(); // return empty list
            }

            List<String> facilityAllBookings = facility.getAvailableSlots(days);
            String[] daysList = days.split(",");
            List<String> matchedAvailabilities = new ArrayList<>();

            for (String booking : facilityAllBookings)
            {
                // String: Mon,23,00 - Tue,01,30
                String bookingDay = booking.substring(0, 3);
                for (String day : daysList)
                {
                    // if day = daysList first three letters,
                    // add it into an array that will list all the availabilities
                    if (bookingDay.equalsIgnoreCase(day.trim()))
                    {
                        matchedAvailabilities.add(booking);
                        break;
                    }
                }
            }
            return matchedAvailabilities;
        }

        public void bookFacility (String facilityName, int startDay, int startHour, int startMinute, int endDay, int endHour, int endMinute)
        {
            // Check if facilityname is in facilities
            // Check if time is valid 
             // Day 
             // Hour
             // Min
             // endtime must be < starttime
            // Check availability function
            // Then make booking -- saving booking into booking hashmap
            
            if (!facilities.containsValue(facilityName)) 
            {
                // throw error
            }

            int bookedStart = startDay * 24 * 60 + startHour * 60 + startMinute;
            int bookedEnd = endDay * 24 * 60 + endHour * 60 + endMinute;
            
            if (bookedEnd < bookedStart) 
            {
                // throw error
            }

            
        }

}
