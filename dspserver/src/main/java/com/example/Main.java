package com.example;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;

public class Main {

        static final int PORT_NUMBER = 12000;
        private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

        public static void main(String[] args) throws Exception {
                // Setup logger with colors and one-line format
                setupLogger();

                LOGGER.info("Starting server on port {}", PORT_NUMBER);
                CustomSocket socket = new AtLeastOnceSocket(PORT_NUMBER);
                socket.createServer();
                LOGGER.info("Server created successfully");

                BookingService bookingService = new BookingService();
                // Define all facilities in an array to avoid repetition
                String[] facilityNames = { "Gym", "Pool", "Spa", "Event Hall", "Lounge" };

                // Create and add each facility in a loop
                for (String name : facilityNames) {
                        Facility facility = new Facility(name);
                        bookingService.addFacility(facility);
                }
                LOGGER.info("Gym, Pool, Spa, Event Hall & Lounge Facilities are added for booking");

                LOGGER.info("BookingService initialized");

                while (true) {
                        LOGGER.debug("Waiting for incoming requests...");
                        SenderResult rawResult = socket.receive();
                        Map<String, Object> result = rawResult.getResult();
                        List<MonitoringClient> clients = bookingService.getAllClients();

                        int service_id = (int) result.get("service_id");
                        String clientInfo = rawResult.getSenderIpAddress() + ":" + rawResult.getSenderPort();
                        LOGGER.info("Received request | Client: {} | Service: {} | ReqID: {}",
                                        clientInfo, getServiceName(service_id), result.get("request_id"));

                        switch (service_id) {
                                case 1:
                                        LOGGER.info("LIST_AVAILABILITY | Facility: {} | Days: {}",
                                                        result.get("facilityName"), result.get("days"));

                                        List<String> facilityAvailability = bookingService.listAvailability(
                                                        (String) result.get("facilityName"),
                                                        (String) result.get("days"));

                                        String availabilities = String.join(":", facilityAvailability);
                                        Map<String, Object> listAvailabilityResp = new HashMap<>();

                                        if (availabilities.startsWith("Error:")) {
                                                listAvailabilityResp.put("errorMessage", availabilities);
                                                socket.send(listAvailabilityResp, (UUID) result.get("request_id"),
                                                                service_id,
                                                                RequestType.ERROR, rawResult.getSenderIpAddress(),
                                                                rawResult.getSenderPort());
                                                LOGGER.info("RESPONSE | LIST_AVAILABILITY | Client: {} | {}",
                                                                clientInfo, availabilities);
                                                break;
                                        } else {
                                                listAvailabilityResp.put("availabilities", availabilities);

                                                socket.send(listAvailabilityResp, (UUID) result.get("request_id"),
                                                                service_id,
                                                                RequestType.RESPONSE, rawResult.getSenderIpAddress(),
                                                                rawResult.getSenderPort());

                                                LOGGER.info("RESPONSE | LIST_AVAILABILITY | Client: {} | Slots: {}",
                                                                clientInfo, availabilities);

                                                break;
                                        }

                                case 2:
                                        LOGGER.info("BOOK_FACILITY | Facility: {} | TimeSlot: {}",
                                                        result.get("facilityName"), result.get("timeSlot"));

                                        Facility newFacility = null;
                                        Map<String, Object> bookFacilityResp = new HashMap<>();

                                        // First, try to find an existing facility
                                        for (Facility facility : bookingService.getAllFacilities()) {
                                                if (facility.getFacilityName().equals(result.get("facilityName"))) {
                                                        newFacility = facility;
                                                        break; // Exit the loop once we find a match
                                                }
                                        }

                                        // If no matching facility was found, create a new one
                                        if (newFacility == null) {
                                                bookFacilityResp.put("errorMessage",
                                                                "Error: Facility name is not found");
                                                socket.send(bookFacilityResp, (UUID) result.get("request_id"),
                                                                service_id, RequestType.ERROR,
                                                                rawResult.getSenderIpAddress(),
                                                                rawResult.getSenderPort());
                                        } else {
                                                String message = bookingService.bookFacility(
                                                                newFacility.getFacilityName(),
                                                                (String) result.get("timeSlot"));

                                                if (message.contains("Error:")) {
                                                        bookFacilityResp.put("errorMessage", message);
                                                        socket.send(bookFacilityResp, (UUID) result.get("request_id"),
                                                                        service_id, RequestType.ERROR,
                                                                        rawResult.getSenderIpAddress(),
                                                                        rawResult.getSenderPort());
                                                        LOGGER.info("RESPONSE | BOOK_FACILITY | Client: {} | {}",
                                                                        clientInfo, message);
                                                } else {
                                                        bookFacilityResp.put("confirmationID", message);
                                                        socket.send(bookFacilityResp, (UUID) result.get("request_id"),
                                                                        service_id, RequestType.RESPONSE,
                                                                        rawResult.getSenderIpAddress(),
                                                                        rawResult.getSenderPort());
                                                        LOGGER.info("RESPONSE | BOOK_FACILITY | Client: {} | ConfirmationID: {}",
                                                                        clientInfo, message);
                                                        notifyCallbackClients(newFacility.getFacilityName(),
                                                                        bookingService,
                                                                        clients, socket);
                                                }
                                        }

                                        break;

                                case 3:
                                        LOGGER.info("EDIT_BOOKING | ConfirmationID: {} | MinuteOffset: {}",
                                                        result.get("confirmationID"), result.get("minuteOffset"));

                                        try {
                                                boolean success = bookingService.editBooking(
                                                                (String) result.get("confirmationID"),
                                                                (int) result.get("minuteOffset"));

                                                Map<String, Object> editBookingResp = new HashMap<>();
                                                editBookingResp.put("success", success);

                                                socket.send(editBookingResp, (UUID) result.get("request_id"),
                                                                service_id,
                                                                RequestType.RESPONSE, rawResult.getSenderIpAddress(),
                                                                rawResult.getSenderPort());

                                                LOGGER.info("RESPONSE | EDIT_BOOKING | Client: {} | Success: {}",
                                                                clientInfo, success);

                                                String facilityName = null;
                                                for (Booking booking : bookingService.getAllBookings()) {
                                                        if (booking.getConfirmationID()
                                                                        .equals(result.get("confirmationID"))) {
                                                                facilityName = booking.getFacilityName();
                                                                break;
                                                        }
                                                }
                                                if (facilityName != null) {
                                                        notifyCallbackClients(facilityName, bookingService, clients,
                                                                        socket);
                                                }

                                        } catch (Exception e) {
                                                Map<String, Object> editBookingResp = new HashMap<>();
                                                editBookingResp.put("errorMessage", e.getMessage());
                                                socket.send(editBookingResp, (UUID) result.get("request_id"),
                                                                service_id,
                                                                RequestType.ERROR, rawResult.getSenderIpAddress(),
                                                                rawResult.getSenderPort());
                                                LOGGER.info("RESPONSE | EDIT_BOOKING | Client: {} | Error: {}",
                                                                clientInfo, e.getMessage());
                                        }

                                        break;

                                case 4:
                                        LOGGER.info("REGISTER_CALLBACK | Facility: {} | MonitoringPeriod: {}min",
                                                        result.get("facilityName"),
                                                        result.get("monitoringPeriodInMinutes"));

                                        boolean registerSuccess = bookingService.registerClient(
                                                        (String) result.get("facilityName"),
                                                        (int) result.get("monitoringPeriodInMinutes"),
                                                        rawResult.getSenderPort(),
                                                        rawResult.getSenderIpAddress());

                                        Map<String, Object> registerCallbackResp = new HashMap<>();

                                        if (registerSuccess) {
                                                registerCallbackResp.put("success", registerSuccess);

                                                socket.send(registerCallbackResp, (UUID) result.get("request_id"),
                                                                service_id,
                                                                RequestType.RESPONSE, rawResult.getSenderIpAddress(),
                                                                rawResult.getSenderPort());
                                        } else {
                                                registerCallbackResp.put("errorMessage", "Failed to register Callback");

                                                socket.send(registerCallbackResp, (UUID) result.get("request_id"),
                                                                service_id,
                                                                RequestType.ERROR, rawResult.getSenderIpAddress(),
                                                                rawResult.getSenderPort());
                                        }
                                        LOGGER.info("RESPONSE | REGISTER_CALLBACK | Client: {} | Success: {}",
                                                        clientInfo, registerSuccess);
                                        break;

                                case 6:
                                        LOGGER.info("CANCEL_BOOKING | ConfirmationID: {}",
                                                        result.get("confirmationID"));

                                        Map<String, Object> cancelBookingResp = new HashMap<>();

                                        try {
                                                String facilityName = null;
                                                for (Booking booking : bookingService.getAllBookings()) {
                                                        if (booking.getConfirmationID()
                                                                        .equals(result.get("confirmationID"))) {
                                                                facilityName = booking.getFacilityName();
                                                                break;
                                                        }
                                                }

                                                boolean cancelBookingSuccess = bookingService.cancelBooking(
                                                                (String) result.get("confirmationID"));
                                                cancelBookingResp.put("success", cancelBookingSuccess);

                                                socket.send(cancelBookingResp, (UUID) result.get("request_id"),
                                                                service_id,
                                                                RequestType.RESPONSE, rawResult.getSenderIpAddress(),
                                                                rawResult.getSenderPort());

                                                LOGGER.info("RESPONSE | CANCEL_BOOKING | Client: {} | Success: {}",
                                                                clientInfo, cancelBookingSuccess);

                                                if (facilityName != null) {
                                                        notifyCallbackClients(facilityName, bookingService, clients,
                                                                        socket);
                                                }

                                        } catch (Exception e) {
                                                cancelBookingResp.put("errorMessage", e.getMessage());
                                                socket.send(cancelBookingResp, (UUID) result.get("request_id"),
                                                                service_id,
                                                                RequestType.ERROR, rawResult.getSenderIpAddress(),
                                                                rawResult.getSenderPort());

                                                LOGGER.info("RESPONSE | CANCEL_BOOKING | Client: {} | Error: {}",
                                                                clientInfo, e.getMessage());
                                        }
                                        break;

                                case 7:
                                        LOGGER.info("EXTEND_BOOKING | ConfirmationID: {} | AdditionalMinutes: {}",
                                                        result.get("confirmationID"), result.get("minuteOffset"));

                                        Map<String, Object> extendBookingResp = new HashMap<>();

                                        try {
                                                boolean extendBookingSuccess = bookingService.extendBooking(
                                                                (String) result.get("confirmationID"),
                                                                (int) result.get("minuteOffset"));

                                                extendBookingResp.put("success", extendBookingSuccess);

                                                socket.send(extendBookingResp, (UUID) result.get("request_id"),
                                                                service_id,
                                                                RequestType.RESPONSE, rawResult.getSenderIpAddress(),
                                                                rawResult.getSenderPort());

                                                LOGGER.info("RESPONSE | EXTEND_BOOKING | Client: {} | Success: {}",
                                                                clientInfo, extendBookingSuccess);

                                                String facilityName = null;
                                                for (Booking booking : bookingService.getAllBookings()) {
                                                        if (booking.getConfirmationID()
                                                                        .equals(result.get("confirmationID"))) {
                                                                facilityName = booking.getFacilityName();
                                                                break;
                                                        }
                                                }
                                                if (facilityName != null) {
                                                        notifyCallbackClients(facilityName, bookingService, clients,
                                                                        socket);
                                                }
                                        } catch (Exception e) {
                                                extendBookingResp.put("errorMessage", e.getMessage());
                                                socket.send(extendBookingResp, (UUID) result.get("request_id"),
                                                                service_id,
                                                                RequestType.ERROR, rawResult.getSenderIpAddress(),
                                                                rawResult.getSenderPort());

                                                LOGGER.info("RESPONSE | EXTEND_BOOKING | Client: {} | Error: {}",
                                                                clientInfo, e.getMessage());
                                        }

                                        break;

                                case 8:
                                        String socketType = (String) result.get("switch");
                                        LOGGER.info("SWITCH_SOCKET | Type: {}", socketType);

                                        if (socketType == null) {
                                                LOGGER.error("SWITCH_SOCKET | socketType is null in the request");
                                                Map<String, Object> errorResp = new HashMap<>();
                                                errorResp.put("errorMessage", "Missing socket type in request");
                                                socket.send(errorResp, (UUID) result.get("request_id"),
                                                                service_id, RequestType.ERROR,
                                                                rawResult.getSenderIpAddress(),
                                                                rawResult.getSenderPort());
                                                break;
                                        }

                                        // Check the request type
                                        RequestType requestType = (RequestType) result.get("request_type");
                                        UUID requestId = (UUID) result.get("request_id");

                                        // Store current socket type for logging
                                        String currentSocketType = socket.getClass().getSimpleName();
                                        LOGGER.info("SWITCH_SOCKET | Current socket: {}, Target socket: {}",
                                                        currentSocketType, socketType);

                                        // Prepare response
                                        Map<String, Object> socketResp = new HashMap<>();

                                        // If this is an AtMostOnceSocket and we're switching to AtLeastOnceSocket
                                        if (currentSocketType.equals("AtMostOnceSocket")
                                                        && socketType.equals("AtLeastOnceSocket")) {
                                                // First send response to the switch request
                                                LOGGER.info("SWITCH_SOCKET | Sending response to switch request");
                                                socketResp.put("message", true);
                                                socket.send(socketResp, requestId, service_id,
                                                                RequestType.RESPONSE, rawResult.getSenderIpAddress(),
                                                                rawResult.getSenderPort());

                                                // Now wait specifically for an ACK before switching
                                                LOGGER.info("SWITCH_SOCKET | Waiting for client ACK before switching...");
                                                try {
                                                        // Set a timeout to ensure we don't wait forever
                                                        socket.setTimeout(5000); // 5 second timeout

                                                        // Instead of trying to receive the ACK (which might be
                                                        // processed internally),
                                                        // just add a small delay to give the socket time to process the
                                                        // ACK
                                                        LOGGER.info("SWITCH_SOCKET | Adding delay to allow ACK processing");
                                                        try {
                                                                Thread.sleep(1000); // 1 second delay
                                                        } catch (InterruptedException e) {
                                                                Thread.currentThread().interrupt();
                                                        }

                                                        // Now close the socket and create the new one
                                                        LOGGER.info("SWITCH_SOCKET | Closing {} after ACK processing delay",
                                                                        currentSocketType);
                                                        socket.close();

                                                        // Create the new AtLeastOnceSocket
                                                        LOGGER.info("Creating AtLeastOnceSocket on port {}",
                                                                        PORT_NUMBER);
                                                        socket = new AtLeastOnceSocket(PORT_NUMBER);
                                                        socket.createServer();
                                                        LOGGER.info("AtLeastOnceSocket server started successfully");

                                                } catch (Exception e) {
                                                        LOGGER.error("SWITCH_SOCKET | Error during ACK wait or socket creation: {}",
                                                                        e.getMessage());
                                                        // Try to recover with default socket
                                                        try {
                                                                socket = new AtLeastOnceSocket(PORT_NUMBER);
                                                                socket.createServer();
                                                                LOGGER.info("Recovered with AtLeastOnceSocket after error");
                                                        } catch (Exception recoveryEx) {
                                                                LOGGER.error("Failed to recover: {}",
                                                                                recoveryEx.getMessage());
                                                                throw recoveryEx;
                                                        }
                                                }
                                        } else {
                                                // For other socket type combinations or same socket type
                                                boolean switchSuccess = false;

                                                try {
                                                        // Close the current socket
                                                        LOGGER.info("SWITCH_SOCKET | Closing current socket: {}",
                                                                        currentSocketType);
                                                        socket.close();

                                                        // Create new socket based on request
                                                        if (socketType.equals("AtLeastOnceSocket")) {
                                                                LOGGER.info("Creating AtLeastOnceSocket on port {}",
                                                                                PORT_NUMBER);
                                                                socket = new AtLeastOnceSocket(PORT_NUMBER);
                                                                socket.createServer();
                                                                LOGGER.info("AtLeastOnceSocket server started successfully");
                                                                switchSuccess = true;
                                                        } else if (socketType.equals("AtMostOnceSocket")) {
                                                                LOGGER.info("Creating AtMostOnceSocket on port {}",
                                                                                PORT_NUMBER);
                                                                socket = new AtMostOnceSocket(PORT_NUMBER);
                                                                socket.createServer();
                                                                LOGGER.info("AtMostOnceSocket server started successfully");
                                                                switchSuccess = true;
                                                        } else {
                                                                LOGGER.error("Unknown socket type: {}", socketType);
                                                                socketResp.put("errorMessage",
                                                                                "Unknown socket type: " + socketType);
                                                                switchSuccess = false;
                                                        }

                                                        // Send response based on success
                                                        if (switchSuccess) {
                                                                socketResp.put("message", true);
                                                                socket.send(socketResp, requestId, service_id,
                                                                                RequestType.RESPONSE,
                                                                                rawResult.getSenderIpAddress(),
                                                                                rawResult.getSenderPort());
                                                                LOGGER.info("RESPONSE | SWITCH_SOCKET | Client: {} | Switched to: {}",
                                                                                clientInfo, socketType);
                                                        } else {
                                                                socketResp.put("message", false);
                                                                socket.send(socketResp, requestId, service_id,
                                                                                RequestType.ERROR,
                                                                                rawResult.getSenderIpAddress(),
                                                                                rawResult.getSenderPort());
                                                                LOGGER.info("RESPONSE | SWITCH_SOCKET | Client: {} | Failed to switch",
                                                                                clientInfo);
                                                        }

                                                } catch (Exception e) {
                                                        LOGGER.error("Failed to switch socket: {}", e.getMessage());
                                                        socketResp.put("errorMessage",
                                                                        "Failed to switch socket: " + e.getMessage());

                                                        // Try to recover
                                                        try {
                                                                socket = new AtLeastOnceSocket(PORT_NUMBER);
                                                                socket.createServer();
                                                                LOGGER.info("Recovered with AtLeastOnceSocket");

                                                                socket.send(socketResp, requestId, service_id,
                                                                                RequestType.ERROR,
                                                                                rawResult.getSenderIpAddress(),
                                                                                rawResult.getSenderPort());
                                                        } catch (Exception recoveryEx) {
                                                                LOGGER.error("Failed to recover: {}",
                                                                                recoveryEx.getMessage());
                                                                throw recoveryEx;
                                                        }
                                                }
                                        }
                                        break;

                                case 9:
                                        LOGGER.warn("SHUTDOWN | Client: {}", clientInfo);
                                        socket.close();
                                        LOGGER.info("Server socket closed");
                                        return;

                                default:
                                        LOGGER.error("UNKNOWN_SERVICE | ID: {} | Client: {}", service_id, clientInfo);
                                        socket.close();
                                        break;
                        }
                }
        }

        private static void setupLogger() {
                // Get the Logger context
                LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

                // Reset any existing configuration
                context.reset();

                // Console Appender with colors
                ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
                consoleAppender.setContext(context);
                consoleAppender.setName("STDOUT");

                // Pattern layout for one-line colored logs
                PatternLayoutEncoder consoleEncoder = new PatternLayoutEncoder();
                consoleEncoder.setContext(context);
                // %highlight - adds ANSI colors based on log level
                // %date - timestamp
                // %yellow - colorize next element yellow
                // %blue - colorize next element blue
                // %green - colorize next element green
                consoleEncoder.setPattern("%highlight(%date{HH:mm:ss.SSS}) [%thread] %-5level %logger{0} - %msg%n");
                consoleEncoder.start();

                consoleAppender.setEncoder(consoleEncoder);
                consoleAppender.start();

                // File Appender for persistent logs (without colors)
                FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
                fileAppender.setContext(context);
                fileAppender.setName("FILE");
                fileAppender.setFile("booking_service.log");
                fileAppender.setAppend(true);

                PatternLayoutEncoder fileEncoder = new PatternLayoutEncoder();
                fileEncoder.setContext(context);
                fileEncoder.setPattern("%date{yyyy-MM-dd HH:mm:ss.SSS} [%-5level] [%thread] %logger{0} - %msg%n");
                fileEncoder.start();

                fileAppender.setEncoder(fileEncoder);
                fileAppender.start();

                // Get the root logger and add appenders
                ch.qos.logback.classic.Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
                rootLogger.addAppender(consoleAppender);
                rootLogger.addAppender(fileAppender);
                rootLogger.setLevel(Level.INFO);

                // Set specific logger levels if needed
                context.getLogger("Main").setLevel(Level.DEBUG);
        }

        private static String getServiceName(int serviceId) {
                switch (serviceId) {
                        case 1:
                                return "LIST_AVAILABILITY";
                        case 2:
                                return "BOOK_FACILITY";
                        case 3:
                                return "EDIT_BOOKING";
                        case 4:
                                return "REGISTER_CALLBACK";
                        case 6:
                                return "CANCEL_BOOKING";
                        case 7:
                                return "EXTEND_BOOKING";
                        case 8:
                                return "SWITCH_SOCKET";
                        case 9:
                                return "SHUTDOWN";
                        default:
                                return "UNKNOWN(" + serviceId + ")";
                }
        }

        /**
         * Notifies monitoring clients about facility availability changes.
         * 
         * @param facilityName   The facility that has updated availability
         * @param bookingService Service to retrieve availability information
         * @param clients        List of clients to be notified
         * @param socket         Socket for sending notifications
         * @throws Exception If an error occurs during the notification process
         */
        private static void notifyCallbackClients(
                        String facilityName,
                        BookingService bookingService,
                        List<MonitoringClient> clients,
                        CustomSocket socket) throws Exception {

                // Validate inputs
                if (facilityName == null || facilityName.isEmpty()) {
                        LOGGER.warn("NOTIFICATION | Invalid facility name: {}", facilityName);
                        return;
                }

                if (clients == null || clients.isEmpty()) {
                        LOGGER.info("NOTIFICATION | Facility: {} | No clients to notify", facilityName);
                        return;
                }

                if (socket == null) {
                        LOGGER.error("NOTIFICATION | Facility: {} | Socket is null", facilityName);
                        return;
                }

                LOGGER.info("NOTIFICATION | Facility: {} | Notifying {} clients", facilityName, clients.size());

                // Log all facilities for debugging
                LOGGER.debug("NOTIFICATION | All facilities: {}",
                                bookingService.getAllFacilities().stream()
                                                .map(f -> f.getFacilityName())
                                                .collect(Collectors.joining(", ")));

                NotificationStats stats = new NotificationStats();

                // Pre-fetch availability data once instead of for each client
                List<String> facilityAvailability;
                String availabilityString = null;
                try {
                        facilityAvailability = bookingService.listAvailability(
                                        facilityName, "Mon,Tue,Wed,Thu,Fri,Sat,Sun");

                        // Handle null case
                        if (facilityAvailability == null) {
                                LOGGER.warn("NOTIFICATION | Null availability returned for facility: {}", facilityName);
                                return;
                        }

                        // Check if the first element indicates an error
                        if (!facilityAvailability.isEmpty() && facilityAvailability.get(0).startsWith("Error:")) {
                                LOGGER.error("NOTIFICATION | Error retrieving availability: {}",
                                                facilityAvailability.get(0));
                                availabilityString = facilityAvailability.get(0);
                        }
                        // Check if the first element indicates "No available slots"
                        else if (facilityAvailability.size() == 1
                                        && facilityAvailability.get(0).startsWith("No available slots")) {
                                LOGGER.info("NOTIFICATION | No available slots for facility: {}", facilityName);
                                // We'll still notify clients that there are no slots
                                availabilityString = "NoSlots:" + facilityName;
                        }
                        // Normal case - we have availability slots
                        else {
                                availabilityString = String.join(":", facilityAvailability);
                        }

                        LOGGER.debug("NOTIFICATION | Facility: {} | Availability data: {}", facilityName,
                                        availabilityString);

                } catch (Exception e) {
                        LOGGER.error("NOTIFICATION | Failed to fetch availability for facility: {} | Error: {} | Stack: {}",
                                        facilityName, e.getMessage(), Arrays.toString(e.getStackTrace()));
                        return;
                }

                // Handle empty availability
                if (availabilityString == null || availabilityString.isEmpty()) {
                        LOGGER.warn("NOTIFICATION | Empty availability for facility: {}", facilityName);
                        return;
                }

                // Process each client
                Iterator<MonitoringClient> iterator = clients.iterator();
                while (iterator.hasNext()) {
                        MonitoringClient client = iterator.next();

                        // Debug logging
                        LOGGER.debug("NOTIFICATION | Processing client: {}:{} for facility {} (client's facility: {})",
                                        client.getClientAddress(), client.getPort(),
                                        facilityName, client.getFacilityName());

                        // Check for null values in client
                        if (client.getClientAddress() == null || client.getFacilityName() == null) {
                                LOGGER.warn("NOTIFICATION | Client has null values: {}", client);
                                iterator.remove();
                                stats.errorCount++;
                                continue;
                        }

                        // Remove expired clients
                        if (client.isExpired()) {
                                LOGGER.debug("NOTIFICATION | Client expired: {}:{} | Registration time: {} | Expiration time: {} minutes | Current time: {}",
                                                client.getClientAddress(), client.getPort(),
                                                new Date(client.getRegistrationTime()),
                                                client.getExpirationTime(),
                                                new Date(System.currentTimeMillis()));
                                iterator.remove();
                                stats.expiredCount++;
                                continue;
                        }

                        // Only notify clients interested in this facility
                        if (!facilityName.equals(client.getFacilityName())) {
                                LOGGER.debug("NOTIFICATION | Client {}:{} not interested in facility {} (wants {})",
                                                client.getClientAddress(), client.getPort(),
                                                facilityName, client.getFacilityName());
                                continue;
                        }

                        // Send notification to client
                        notifyClient(client, facilityName, availabilityString, socket, stats);
                }

                LOGGER.info("NOTIFICATION | Complete | Facility: {} | Notified: {} | Expired: {} | Errors: {}",
                                facilityName, stats.notifiedCount, stats.expiredCount, stats.errorCount);
        }

        /**
         * Helper method to notify an individual client
         */
        private static void notifyClient(
                        MonitoringClient client,
                        String facilityName,
                        String availabilityString,
                        CustomSocket socket,
                        NotificationStats stats) {

                UUID requestId = UUID.randomUUID();
                Map<String, Object> response = new HashMap<>();

                try {
                        LOGGER.debug("NOTIFICATION | Attempting to notify client: {}:{} for facility: {}",
                                        client.getClientAddress(), client.getPort(), facilityName);

                        if (availabilityString.startsWith("Error:") || availabilityString.startsWith("NoSlots:")) {
                                // Handle error in availability data
                                response.put("errorMessage", availabilityString);

                                LOGGER.debug("NOTIFICATION | About to send error via socket to: {}:{} | requestId: {} | response: {}",
                                                client.getClientAddress(), client.getPort(), requestId, response);

                                socket.send(response, requestId, 4, // 4 corresponds to REGISTER_CALLBACK
                                                RequestType.ERROR, client.getClientAddress(), client.getPort());

                                LOGGER.info("NOTIFICATION | Client: {}:{} | Sent error: {}",
                                                client.getClientAddress(), client.getPort(), availabilityString);
                                stats.errorCount++;
                        } else {
                                // Send successful availability data
                                response.put("availabilities", availabilityString);

                                LOGGER.info("NOTIFICATION | About to send success via socket to: {}:{} | requestId: {} | responseSize: {}",
                                                client.getClientAddress(), client.getPort(), requestId,
                                                response.size());

                                socket.send(response, requestId, 5, // 5 correspond to NotifyCallback
                                                RequestType.REQUEST, client.getClientAddress(), client.getPort());

                                LOGGER.debug("NOTIFICATION | Socket send completed successfully");

                                LOGGER.info("NOTIFICATION | Client: {}:{} | Facility: {} | Slots: {}",
                                                client.getClientAddress(), client.getPort(), facilityName,
                                                availabilityString.length() > 100
                                                                ? availabilityString.substring(0, 100) + "..."
                                                                : availabilityString);
                                stats.notifiedCount++;
                        }
                } catch (Exception e) {
                        LOGGER.error("NOTIFICATION | Failed to notify client: {}:{} | Error: {} | Stack: {}",
                                        client.getClientAddress(), client.getPort(),
                                        e.getMessage(), Arrays.toString(e.getStackTrace()));
                        stats.errorCount++;
                }
        }

        /**
         * Helper class to track notification statistics
         */
        private static class NotificationStats {
                int expiredCount = 0;
                int notifiedCount = 0;
                int errorCount = 0;
        }
}