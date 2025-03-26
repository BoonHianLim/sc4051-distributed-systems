package com.example;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Main {

    static final int PORT_NUMBER = 12000;

    public static void main(String[] args) throws Exception {
        CustomSocket socket = new AtLeastOnceSocket(PORT_NUMBER);
        socket.createServer();
        BookingService bookingService = new BookingService();

        while (true) {
            SenderResult rawResult = socket.receive();
            Map<String, Object> result = rawResult.getResult();
            List<MonitoringClient> clients = bookingService.getAllClients();
            
            int service_id = (int) result.get("service_id");
            System.out.println("Service_id " + service_id);
            switch (service_id) {
                case 1:
                    List<String> facilityAvailability = bookingService.listAvailability((String) result.get("facilityName"), (String) result.get("days"));
                    String availabilities = String.join(":", facilityAvailability);
                    System.out.println(availabilities);
                    Map<String, Object> listAvailabilityResp = new HashMap<String, Object>();
                    listAvailabilityResp.put("availabilities", availabilities);
                    socket.send(listAvailabilityResp, (UUID) result.get("request_id"), service_id, RequestType.RESPONSE, rawResult.getSenderIpAddress(), rawResult.getSenderPort());
                    break;
                case 2:
                    Facility newFacility = new Facility((String) result.get("facilityName"));
                    bookingService.addFacility(newFacility);
                    String confirmationID = bookingService.bookFacility(newFacility.getFacilityName(), (String) result.get("timeSlot"));
                    Map<String, Object> bookFacilityResp = new HashMap<String, Object>();
                    bookFacilityResp.put("confirmationID", confirmationID);
                    socket.send(bookFacilityResp, (UUID) result.get("request_id"), service_id, 
                            RequestType.RESPONSE, rawResult.getSenderIpAddress(), rawResult.getSenderPort());
                    notifyCallbackClients(newFacility.getFacilityName(), bookingService, result, clients, rawResult, socket);
                    break;
                case 3:
                    boolean success = bookingService.editBooking((String) result.get("confirmationID"), (int) result.get("minuteOffset"));
                    Map<String, Object> editBookingResp = new HashMap<String, Object>();
                    editBookingResp.put("success", success);
                    socket.send(editBookingResp, (UUID) result.get("request_id"), service_id, 
                            RequestType.RESPONSE, rawResult.getSenderIpAddress(), rawResult.getSenderPort());
                    break;
                case 4:
                    // registerCallback
                    boolean registerSuccess = bookingService.registerClient((String) result.get("facilityName"), (int) result.get("monitoringPeriodInMinutes"), rawResult.getSenderPort(), rawResult.getSenderIpAddress());
                    Map<String, Object> registerCallbackResp = new HashMap<String, Object>();
                    registerCallbackResp.put("success", registerSuccess);
                    socket.send(registerCallbackResp, (UUID) result.get("request_id"), service_id, 
                            RequestType.RESPONSE, rawResult.getSenderIpAddress(), rawResult.getSenderPort());
                    break;
                case 6:
                    boolean cancelBookingSuccess = bookingService.cancelBooking((String) result.get("confirmationID"));
                    Map<String, Object> cancelBookingResp = new HashMap<String, Object>();
                    cancelBookingResp.put("success", cancelBookingSuccess);
                    socket.send(cancelBookingResp, (UUID) result.get("request_id"), service_id, 
                            RequestType.RESPONSE, rawResult.getSenderIpAddress(), rawResult.getSenderPort());
                    break;
                case 7:
                    boolean extendBookingSuccess = bookingService.extendBooking((String) result.get("confirmationID"), (int) result.get("additionalMinutes"));
                    Map<String, Object> extendBookingResp = new HashMap<String, Object>();
                    extendBookingResp.put("success", extendBookingSuccess);
                    socket.send(extendBookingResp, (UUID) result.get("request_id"), service_id, 
                            RequestType.RESPONSE, rawResult.getSenderIpAddress(), rawResult.getSenderPort());
                    break;
                case 8:
                    String socketType = (String) result.get("switch"); // either AtLeastOnceSocket or AtMostOnceSocket
                    // if socketType == AtLeastOnceSocket
                    // make sure the socket uses AtLeastOnceSocket
                    // if socketType == AtMostOnceSocket
                    // make sure the socket uses
                case 9:
                    socket.close();
                default:
                    break;
            }
        } 
    }

    private static void notifyCallbackClients(String facilityName, BookingService bookingService, Map<String, Object> result, List<MonitoringClient> clients, SenderResult rawResult, CustomSocket socket) throws IOException {
        for (MonitoringClient client: clients) {
            if (client.isExpired()) {
                clients.remove(client);
            }
            if (facilityName.equals(client.getFacilityName())) {
                List<String> facilityAvailability = bookingService.listAvailability((String) result.get("facilityName"), (String) result.get("days"));
                String availabilities = String.join(":", facilityAvailability);
                Map<String, Object> listAvailabilityResp = new HashMap<String, Object>();
                listAvailabilityResp.put("availabilities", availabilities);
                socket.send(listAvailabilityResp, (UUID) result.get("request_id"), (int) result.get("service_id"),  RequestType.RESPONSE, rawResult.getSenderIpAddress(), rawResult.getSenderPort());
            }
        }
    }
}
