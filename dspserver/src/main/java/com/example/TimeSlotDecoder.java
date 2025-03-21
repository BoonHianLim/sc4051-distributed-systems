package com.example;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TimeSlotDecoder {
    private String startDay;
    private String endDay;
    private int startHour;
    private int endHour;
    private int startMin;
    private int endMin;
    private int durationMinutes;

    // Map
    public static final Map<String, Integer> DAY_TO_INDEX = Map.of(
        "Mon", 0,
        "Tue", 1,
        "Wed", 2,
        "Thu", 3,
        "Fri", 4,
        "Sat", 5,
        "Sun", 6
    );

    // Constructor
    public TimeSlotDecoder (String input)
    {
        parseInput(input);
        calculateDuration();
    }

    public void parseInput(String input)
    {
        String[] parts = input.split(" - ");
        if (parts.length != 2)
        {
            throw new IllegalArgumentException("Invalid input format: " + input);
        }

        String[] startParts = parts[0].split(",");
        String[] endParts = parts[1].split(",");

        if (startParts.length != 3 || endParts.length != 3) {
            throw new IllegalArgumentException("Invalid time format in input: " + input);
        }

        startDay = startParts[0];
        startHour = Integer.parseInt(startParts[1]);
        startMin = Integer.parseInt(startParts[2]);

        endDay = endParts[0];
        endHour = Integer.parseInt(endParts[1]);
        endMin = Integer.parseInt(endParts[2]);
    }

    private void calculateDuration()
    {
        int startTotalMinutes = DAY_TO_INDEX.get(startDay) * 24 * 60 + startHour * 60 + startMin;
        int endTotalMinutes = DAY_TO_INDEX.get(endDay) * 24 * 60 + endHour * 60 + endMin;

        durationMinutes = endTotalMinutes - startTotalMinutes;

        if (durationMinutes < 0)
        {
            throw new IllegalArgumentException("End time must be after start time.");   
        }
    }

    public String toString() {
        return getDecodedTimeSlot().toString();
    }

    public List<Object> getDecodedTimeSlot() {
        return Arrays.asList(
            startDay, startHour, startMin,
            endDay, endHour, endMin,
            durationMinutes
        );
    }
}