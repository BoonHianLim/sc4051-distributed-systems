package com.example;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
                    listAvailabilityResp.put("availabilities", availabilities);
                    
                    socket.send(listAvailabilityResp, (UUID) result.get("request_id"), service_id, 
                               RequestType.RESPONSE, rawResult.getSenderIpAddress(), rawResult.getSenderPort());
                    
                    LOGGER.info("RESPONSE | LIST_AVAILABILITY | Client: {} | Slots: {}", 
                               clientInfo, availabilities);
                    break;
                    
                case 2:
                    LOGGER.info("BOOK_FACILITY | Facility: {} | TimeSlot: {}", 
                               result.get("facilityName"), result.get("timeSlot"));
                    
                    Facility newFacility = new Facility((String) result.get("facilityName"));
                    bookingService.addFacility(newFacility);
                    
                    String confirmationID = bookingService.bookFacility(
                            newFacility.getFacilityName(), 
                            (String) result.get("timeSlot"));
                    
                    Map<String, Object> bookFacilityResp = new HashMap<>();
                    bookFacilityResp.put("confirmationID", confirmationID);
                    
                    socket.send(bookFacilityResp, (UUID) result.get("request_id"), service_id, 
                               RequestType.RESPONSE, rawResult.getSenderIpAddress(), rawResult.getSenderPort());
                    
                    LOGGER.info("RESPONSE | BOOK_FACILITY | Client: {} | ConfirmationID: {}", 
                               clientInfo, confirmationID);
                    
                    notifyCallbackClients(newFacility.getFacilityName(), bookingService, 
                                         result, clients, rawResult, socket);
                    break;
                    
                case 3:
                    LOGGER.info("EDIT_BOOKING | ConfirmationID: {} | MinuteOffset: {}", 
                               result.get("confirmationID"), result.get("minuteOffset"));
                    
                    boolean success = bookingService.editBooking(
                            (String) result.get("confirmationID"), 
                            (int) result.get("minuteOffset"));
                    
                    Map<String, Object> editBookingResp = new HashMap<>();
                    editBookingResp.put("success", success);
                    
                    socket.send(editBookingResp, (UUID) result.get("request_id"), service_id, 
                               RequestType.RESPONSE, rawResult.getSenderIpAddress(), rawResult.getSenderPort());
                    
                    LOGGER.info("RESPONSE | EDIT_BOOKING | Client: {} | Success: {}", 
                               clientInfo, success);
                    break;
                    
                case 4:
                    LOGGER.info("REGISTER_CALLBACK | Facility: {} | MonitoringPeriod: {}min", 
                               result.get("facilityName"), result.get("monitoringPeriodInMinutes"));
                    
                    boolean registerSuccess = bookingService.registerClient(
                            (String) result.get("facilityName"), 
                            (int) result.get("monitoringPeriodInMinutes"), 
                            rawResult.getSenderPort(), 
                            rawResult.getSenderIpAddress());
                    
                    Map<String, Object> registerCallbackResp = new HashMap<>();
                    registerCallbackResp.put("success", registerSuccess);
                    
                    socket.send(registerCallbackResp, (UUID) result.get("request_id"), service_id, 
                               RequestType.RESPONSE, rawResult.getSenderIpAddress(), rawResult.getSenderPort());
                    
                    LOGGER.info("RESPONSE | REGISTER_CALLBACK | Client: {} | Success: {}", 
                               clientInfo, registerSuccess);
                    break;
                    
                case 6:
                    LOGGER.info("CANCEL_BOOKING | ConfirmationID: {}", result.get("confirmationID"));
                    
                    boolean cancelBookingSuccess = bookingService.cancelBooking(
                            (String) result.get("confirmationID"));
                    
                    Map<String, Object> cancelBookingResp = new HashMap<>();
                    cancelBookingResp.put("success", cancelBookingSuccess);
                    
                    socket.send(cancelBookingResp, (UUID) result.get("request_id"), service_id, 
                               RequestType.RESPONSE, rawResult.getSenderIpAddress(), rawResult.getSenderPort());
                    
                    LOGGER.info("RESPONSE | CANCEL_BOOKING | Client: {} | Success: {}", 
                               clientInfo, cancelBookingSuccess);
                    break;
                    
                case 7:
                    LOGGER.info("EXTEND_BOOKING | ConfirmationID: {} | AdditionalMinutes: {}", 
                               result.get("confirmationID"), result.get("additionalMinutes"));
                    
                    boolean extendBookingSuccess = bookingService.extendBooking(
                            (String) result.get("confirmationID"), 
                            (int) result.get("additionalMinutes"));
                    
                    Map<String, Object> extendBookingResp = new HashMap<>();
                    extendBookingResp.put("success", extendBookingSuccess);
                    
                    socket.send(extendBookingResp, (UUID) result.get("request_id"), service_id, 
                               RequestType.RESPONSE, rawResult.getSenderIpAddress(), rawResult.getSenderPort());
                    
                    LOGGER.info("RESPONSE | EXTEND_BOOKING | Client: {} | Success: {}", 
                               clientInfo, extendBookingSuccess);
                    break;
                    
                case 8:
                    String socketType = (String) result.get("switch");
                    LOGGER.info("SWITCH_SOCKET | Type: {}", socketType);
                    // Implementation pending
                    LOGGER.warn("Socket switch implementation pending");
                    break;
                    
                case 9:
                    LOGGER.warn("SHUTDOWN | Client: {}", clientInfo);
                    socket.close();
                    LOGGER.info("Server socket closed");
                    break;
                    
                default:
                    LOGGER.error("UNKNOWN_SERVICE | ID: {} | Client: {}", service_id, clientInfo);
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
            case 1: return "LIST_AVAILABILITY";
            case 2: return "BOOK_FACILITY";
            case 3: return "EDIT_BOOKING";
            case 4: return "REGISTER_CALLBACK";
            case 6: return "CANCEL_BOOKING";
            case 7: return "EXTEND_BOOKING";
            case 8: return "SWITCH_SOCKET";
            case 9: return "SHUTDOWN";
            default: return "UNKNOWN(" + serviceId + ")";
        }
    }
    
    private static void notifyCallbackClients(String facilityName, BookingService bookingService, 
                                             Map<String, Object> result, List<MonitoringClient> clients,
                                             SenderResult rawResult, CustomSocket socket) throws Exception {
        LOGGER.info("NOTIFICATION | Facility: {} | Notifying {} clients", 
                   facilityName, clients.size());
        
        int expiredCount = 0;
        int notifiedCount = 0;
        
        for (MonitoringClient client : clients) {
            if (client.isExpired()) {
                LOGGER.debug("NOTIFICATION | Client expired: {}:{}", 
                           client.getClientAddress(), client.getPort());
                clients.remove(client);
                expiredCount++;
                continue;
            }
            
            if (facilityName.equals(client.getFacilityName())) {
                LOGGER.debug("NOTIFICATION | Sending update to client: {}:{}", 
                client.getClientAddress(), client.getPort());
                
                List<String> facilityAvailability = bookingService.listAvailability(
                        (String) result.get("facilityName"), 
                        (String) result.get("days"));
                        
                String availabilities = String.join(":", facilityAvailability);
                Map<String, Object> listAvailabilityResp = new HashMap<>();
                listAvailabilityResp.put("availabilities", availabilities);
                
                socket.send(listAvailabilityResp, 
                           (UUID) result.get("request_id"), 
                           (int) result.get("service_id"),  
                           RequestType.RESPONSE, 
                           rawResult.getSenderIpAddress(), 
                           rawResult.getSenderPort());
                           
                notifiedCount++;
            }
        }
        
        LOGGER.info("NOTIFICATION | Complete | Facility: {} | Notified: {} | Expired: {}", 
                   facilityName, notifiedCount, expiredCount);
    }
}