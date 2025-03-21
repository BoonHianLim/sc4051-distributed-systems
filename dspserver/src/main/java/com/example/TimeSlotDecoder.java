package com.example;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TimeSlotDecoder {
    public static final Map<String, Integer> DAY_TO_INDEX = Map.of(
            "Mon", 0,
            "Tue", 1,
            "Wed", 2,
            "Thu", 3,
            "Fri", 4,
            "Sat", 5,
            "Sun", 6);

    private static final Map<String, DayOfWeek> DAY_TO_ENUM = Map.of(
            "Mon", DayOfWeek.MONDAY,
            "Tue", DayOfWeek.TUESDAY,
            "Wed", DayOfWeek.WEDNESDAY,
            "Thu", DayOfWeek.THURSDAY,
            "Fri", DayOfWeek.FRIDAY,
            "Sat", DayOfWeek.SATURDAY,
            "Sun", DayOfWeek.SUNDAY);

    private final DayOfWeek startDay;
    private final DayOfWeek endDay;
    private final int startHour;
    private final int endHour;
    private final int startMin;
    private final int endMin;
    private final int durationMinutes;

    public TimeSlotDecoder(String input) {
        String[] times = parseTimeSlot(input);
        TimePoint start = parseTimePoint(times[0]);
        TimePoint end = parseTimePoint(times[1]);

        this.startDay = start.day;
        this.startHour = start.hour;
        this.startMin = start.minute;

        this.endDay = end.day;
        this.endHour = end.hour;
        this.endMin = end.minute;

        this.durationMinutes = calculateDuration(start, end);
    }

    private String[] parseTimeSlot(String input) {
        String[] parts = input.split(" - ");
        if (parts.length != 2) {
            throw new IllegalArgumentException(
                    "Invalid input format. Expected 'Day,Hour,Minute - Day,Hour,Minute' but got: " + input);
        }
        return parts;
    }

    private TimePoint parseTimePoint(String timePoint) {
        String[] parts = timePoint.split(",");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid time format. Expected 'Day,Hour,Minute' but got: " + timePoint);
        }

        String day = parts[0];
        if (!DAY_TO_ENUM.containsKey(day)) {
            throw new IllegalArgumentException("Invalid day: " + day);
        }

        try {
            int hour = Integer.parseInt(parts[1]);
            int minute = Integer.parseInt(parts[2]);

            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                throw new IllegalArgumentException(
                        String.format("Invalid time values. Hours must be 0-23, minutes must be 0-59. Got: %d:%d", hour,
                                minute));
            }

            return new TimePoint(DAY_TO_ENUM.get(day), hour, minute);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Hour and minute must be integers: " + timePoint, e);
        }
    }

    private int calculateDuration(TimePoint start, TimePoint end) {
        LocalDateTime startDateTime = LocalDateTime.of(2023, 1, start.day.getValue(), start.hour, start.minute);
        LocalDateTime endDateTime = LocalDateTime.of(2023, 1, end.day.getValue(), end.hour, end.minute);

        // Adjust for week wrap-around
        if (endDateTime.isBefore(startDateTime)) {
            endDateTime = endDateTime.plusDays(7);
        }

        Duration duration = Duration.between(startDateTime, endDateTime);
        int minutes = (int) duration.toMinutes();

        if (minutes < 0) {
            throw new IllegalArgumentException("End time must be after start time.");
        }

        return minutes;
    }

    public List<Object> getDecodedTimeSlot() {
        return List.of(
                startDay.toString().substring(0, 3),
                startHour,
                startMin,
                endDay.toString().substring(0, 3),
                endHour,
                endMin,
                durationMinutes);
    }

    @Override
    public String toString() {
        return getDecodedTimeSlot().toString();
    }

    // Getters
    public String getStartDay() {
        return startDay.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
    }

    public String getEndDay() {
        return endDay.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
    }

    public int getStartHour() {
        return startHour;
    }

    public int getEndHour() {
        return endHour;
    }

    public int getStartMin() {
        return startMin;
    }

    public int getEndMin() {
        return endMin;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    // Helper class for internal use
    private static class TimePoint {
        final DayOfWeek day;
        final int hour;
        final int minute;

        TimePoint(DayOfWeek day, int hour, int minute) {
            this.day = day;
            this.hour = hour;
            this.minute = minute;
        }
    }
}