package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BookingServiceTest {
    
    private BookingService bookingService;
    private Facility gymFacility;
    private Facility poolFacility;
    
    @BeforeEach
    public void setUp() {
        // Initialize the booking service
        bookingService = new BookingService();
        
        // Create test facilities
        gymFacility = new Facility("Gym");
        poolFacility = new Facility("Swimming Pool");
        
        // Add facilities to the booking service
        bookingService.addFacility(gymFacility);
        bookingService.addFacility(poolFacility);
    }
    
    @Test
    public void testAddFacility() {
        Facility tennisCourt = new Facility("Tennis Court");
        assertTrue(bookingService.addFacility(tennisCourt));
        
        // Should not allow null facilities
        assertThrows(IllegalArgumentException.class, () -> bookingService.addFacility(null));
    }
    
    @Test
    public void testListAvailabilityWithNoFacilityName() {
        List<String> result = bookingService.listAvailability("", "Mon, Tue");
        assertEquals(1, result.size());
        assertTrue(result.get(0).startsWith("Error:"));
    }
    
    @Test
    public void testListAvailabilityWithNonExistingFacility() {
        List<String> result = bookingService.listAvailability("NonExistingFacility", "Mon, Tue");
        assertEquals(1, result.size());
        assertTrue(result.get(0).startsWith("Error:"));
    }
    
    @Test
    public void testListAvailabilityWithInvalidDays() {
        List<String> result = bookingService.listAvailability("Gym", "Monday, Tuesday");
        assertEquals(1, result.size());
        assertTrue(result.get(0).startsWith("Error:"));
    }
    
    @Test
    public void testListAvailabilityWithValidInput() {
        // First make sure we have some bookings
        bookingService.bookFacility("Gym", "Mon,10,0 - Mon,11,0", "John Doe");
        bookingService.bookFacility("Gym", "Tue,14,0 - Tue,15,0", "Jane Smith");
        
        // Test listing availability
        List<String> result = bookingService.listAvailability("Gym", "Mon, Tue");
        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(slot -> slot.startsWith("Mon")));
    }
    
    @Test
    public void testBookFacility() {
        // Book a facility with valid inputs
        String result = bookingService.bookFacility("Gym", "Mon,10,0 - Mon,11,0", "John Doe");
        assertTrue(result.contains("Booking confirmed"));
        
        // Should not allow empty facility name
        result = bookingService.bookFacility("", "Mon,10,0 - Mon,11,0", "John Doe");
        assertTrue(result.startsWith("Error:"));
        
        // Should not allow empty time slot
        result = bookingService.bookFacility("Gym", "", "John Doe");
        assertTrue(result.startsWith("Error:"));
        
        // Should not allow empty client name
        result = bookingService.bookFacility("Gym", "Mon,10,0 - Mon,11,0", "");
        assertTrue(result.startsWith("Error:"));
        
        // Should not allow booking non-existing facility
        result = bookingService.bookFacility("NonExistingFacility", "Mon,10,0 - Mon,11,0", "John Doe");
        assertTrue(result.startsWith("Error:"));
        
        // Should not allow double booking the same time slot
        result = bookingService.bookFacility("Gym", "Mon,10,0 - Mon,11,0", "Jane Smith");
        assertTrue(result.startsWith("Error:"));
    }
    
    @Test
    public void testEditBooking() {
        // First, book a facility
        String bookingResult = bookingService.bookFacility("Gym", "Mon,10,0 - Mon,11,0", "John Doe");
        String confirmationId = bookingResult.substring(bookingResult.lastIndexOf(":") + 2);
        
        // Test shifting the booking by 30 minutes
        assertTrue(bookingService.editBooking(confirmationId, 30));
        
        // Book another slot that would overlap if we tried to shift the first booking again
        bookingService.bookFacility("Gym", "Mon,11,30 - Mon,12,30", "Jane Smith");
        
        // Should fail because the new time slot would overlap with an existing booking
        assertThrows(RuntimeException.class, () -> bookingService.editBooking(confirmationId, 30));
        
        // Should fail with invalid confirmation ID
        assertThrows(IllegalArgumentException.class, () -> bookingService.editBooking("invalid-id", 30));
    }
    
    @Test
    public void testCancelBookingByConfirmationId() {
        // First, book a facility
        String bookingResult = bookingService.bookFacility("Gym", "Mon,10,0 - Mon,11,0", "John Doe");
        String confirmationId = bookingResult.substring(bookingResult.lastIndexOf(":") + 2);
        
        // Test cancelling the booking
        assertTrue(bookingService.cancelBooking(confirmationId));
        
        // Booking should no longer exist
        assertThrows(IllegalArgumentException.class, () -> bookingService.cancelBooking(confirmationId));
        
        // Should fail with null confirmation ID
        assertThrows(IllegalArgumentException.class, () -> bookingService.cancelBooking(null));
        
        // Should fail with empty confirmation ID
        assertThrows(IllegalArgumentException.class, () -> bookingService.cancelBooking(""));
        
        // Should fail with invalid confirmation ID
        assertThrows(IllegalArgumentException.class, () -> bookingService.cancelBooking("invalid-id"));
    }
    
    @Test
    public void testExtendBooking() {
        // First, book a facility
        String bookingResult = bookingService.bookFacility("Gym", "Mon,10,0 - Mon,11,0", "John Doe");
        String confirmationId = bookingResult.substring(bookingResult.lastIndexOf(":") + 2);
        
        // Test extending the booking by 15 minutes - should work
        assertTrue(bookingService.extendBooking(confirmationId, 15));
        
        // Book another slot that would overlap if we tried to extend the first booking further
        bookingService.bookFacility("Gym", "Mon,11,30 - Mon,12,30", "Jane Smith");
        
        // Now try extending it further - should fail because it would overlap with Jane's booking
        assertThrows(IllegalStateException.class, () -> bookingService.extendBooking(confirmationId, 30));
        
        // Should fail with negative minutes
        assertThrows(IllegalArgumentException.class, () -> bookingService.extendBooking(confirmationId, -30));
        
        // Should fail with invalid confirmation ID
        assertThrows(IllegalArgumentException.class, () -> bookingService.extendBooking("invalid-id", 30));
    }
    
    @Test
    public void testGetAllBookings() {
        // Initially, there should be no bookings
        assertEquals(0, bookingService.getAllBookings().size());
        
        // Add a few bookings
        bookingService.bookFacility("Gym", "Mon,10,0 - Mon,11,0", "John Doe");
        bookingService.bookFacility("Swimming Pool", "Tue,14,0 - Tue,15,0", "Jane Smith");
        
        // Should have 2 bookings now
        assertEquals(2, bookingService.getAllBookings().size());
    }
    
    @Test
    public void testGetAllFacilities() {
        // Should have 2 facilities from setup
        assertEquals(2, bookingService.getAllFacilities().size());
        
        // Add another facility
        bookingService.addFacility(new Facility("Basketball Court"));
        
        // Should have 3 facilities now
        assertEquals(3, bookingService.getAllFacilities().size());
    }
}