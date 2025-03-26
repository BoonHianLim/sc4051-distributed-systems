package com.example;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Main {

    static final int PORT_NUMBER = 12000;

    public static void main(String[] args) throws Exception {
        CustomSocket socket = new AtLeastOnceSocket(PORT_NUMBER);
        socket.createServer();
        SenderResult rawResult = socket.receive();
        System.out.println(rawResult.getResult());
        Map<String, Object> result = rawResult.getResult();
        BookingService bookingService = new BookingService();
        // Create test facilities
        Facility gymFacility = new Facility("Gym");
        Facility poolFacility = new Facility("Swimming Pool");
        
        // Add facilities to the booking service
        bookingService.addFacility(gymFacility);
        bookingService.addFacility(poolFacility);

        bookingService.bookFacility("Gym", "Mon,10,0 - Mon,11,0", "John Doe");
        bookingService.bookFacility("Gym", "Tue,14,0 - Tue,15,0", "Jane Smith");
        
        int service_id = (int) result.get("service_id");

        switch (service_id) {
            case 1:
                List<String> facilityAvailability = bookingService.listAvailability((String) result.get("facilityName"), (String) result.get("days"));
                String availabilities = String.join(":", facilityAvailability);
                Map<String, Object> listAvailabilityResp = new HashMap<String, Object>();
                listAvailabilityResp.put("availabilities", availabilities);
                socket.send(listAvailabilityResp, (UUID) result.get("request_id"), service_id, RequestType.RESPONSE, rawResult.getSenderIpAddress(), rawResult.getSenderPort());
                break;

            default:
                break;
        }
        socket.close();
    }
}
