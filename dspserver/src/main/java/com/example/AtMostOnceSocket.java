package com.example;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of CustomSocket that guarantees at-most-once message delivery.
 * Tracks received message IDs to detect and discard duplicates.
 * Uses a history table to store responses and clear them upon ACK reception.
 */
public class AtMostOnceSocket extends CustomSocket {
    // History table to store responses by requestID
    private final ConcurrentHashMap<UUID, ResponseInfo> historyTable = new ConcurrentHashMap<>();

    /**
     * Creates an AtMostOnceSocket with the specified port number.
     * 
     * @param portNumber The port number for this socket
     */
    public AtMostOnceSocket(int portNumber) {
        super(portNumber);
    }

    @Override
    public void send(Map<String, Object> message, UUID requestId, int serviceId,
            RequestType requestType, InetAddress destinationAddress, int destinationPort)
            throws IOException {
                try {
                    // Create message and marshal it
                    Parser.Message parsedMessage = createMessage(message, serviceId, requestId, requestType);
                    byte[] data = parser.marshall(parsedMessage);

                    // If this is a response or error, store it in the history table
                    if (requestType == RequestType.RESPONSE || requestType == RequestType.ERROR) {
                        // Store the response data in the history table for later use
                        ResponseInfo responseInfo = new ResponseInfo(data, destinationAddress, destinationPort, requestType);
                        historyTable.put(requestId, responseInfo);

                        // Send the response and wait for the ACK (handled in the receive method)
                        sendDatagram(data, destinationAddress, destinationPort);
                    }
                    else if (requestType == RequestType.REQUEST) {
                        // Simply send the request
                        sendDatagram(data, destinationAddress, destinationPort);
                    }
                    else if (requestType == RequestType.ACK) {
                        // For ACK messages, just send them without storing
                        sendDatagram(data, destinationAddress, destinationPort);
                    }
                } catch (Exception e) {
                    throw new IOException("Failed to send message", e);
                }
            }

    @Override
    public SenderResult receive() throws IOException {
        while (true) {
            try {
                DatagramPacket packet = receiveDatagram();
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());

                // Parse the received message
                Parser.Message message = parser.unmarshall(data);

                System.out.println("Parsed AtMostOnce message: serviceId = " + message.getServiceId() + " requestType=" + message.getRequestType() + " requestId=" + message.getRequestId() + " data=" + message.getData());

                UUID requestId = message.getRequestId();
                int serviceId = message.getServiceId();
                RequestType requestType = message.getRequestType();
                
                // Handle ACK messages
                if (requestType == RequestType.ACK) {
                    // Check if there's an entry in the history table for this request ID
                    ResponseInfo removedResponse = historyTable.remove(requestId);
                    if (removedResponse != null) {
                        System.out.println("AtMostOnceSocket: Received ACK for request ID " + requestId + 
                                          " with service ID " + serviceId + " - Cleared from history table");
                    } else {
                        System.out.println("AtMostOnceSocket: Received ACK for request ID " + requestId + 
                                          " with service ID " + serviceId + " - No entry found in history table");
                    }
                    continue; // Continue waiting for non-ACK messages
                }
                
                // Handle incoming requests
                if (requestType == RequestType.REQUEST) {
                    // Check if we've seen this request and have a response in the history table
                    ResponseInfo storedResponse = historyTable.get(requestId);
                    if (storedResponse != null) {
                        // We've already processed this request - resend the stored response
                        sendDatagram(storedResponse.data, packet.getAddress(), packet.getPort());
                        continue; // Skip this message and wait for a new one
                    }
                }
                
                // Handle incoming responses - send an ACK
                if (requestType == RequestType.RESPONSE || requestType == RequestType.ERROR) {
                    // Send ACK for the response
                    sendAcknowledgment(requestId, serviceId, packet.getAddress(), packet.getPort());
                }

                // Convert to the expected response format for application layer
                Map<String, Object> resultMap = new HashMap<>(message.getData());
                resultMap.put("request_id", requestId);
                resultMap.put("service_id", serviceId);
                resultMap.put("request_type", requestType);
                
                return new SenderResult(packet.getAddress(), packet.getPort(), resultMap);

            } catch (Exception e) {
                throw new IOException("Failed to receive message", e);
            }
        }
    }

    /**
     * Sends an acknowledgment for a received message.
     * 
     * @param requestId The ID of the message being acknowledged
     * @param serviceId The service ID of the original message
     * @param address The address to send the acknowledgment to
     * @param port The port to send the acknowledgment to
     * @throws IOException If sending fails
     */
    public void sendAcknowledgment(UUID requestId, int serviceId, InetAddress address, int port) throws IOException {
        try {
            // Create a simple acknowledgment message
            Map<String, Object> ackData = new HashMap<>();
            ackData.put("ack", true);
            
            Parser.Message ackMessage = new Parser.Message(
                    requestId, serviceId, RequestType.ACK, "ACK", ackData);
            
            byte[] ackBytes = parser.marshall(ackMessage);
            sendDatagram(ackBytes, address, port);
            
        } catch (Exception e) {
            throw new IOException("Failed to send acknowledgment", e);
        }
    }


    /**
     * Class representing a response stored in the history table.
     */
    private static class ResponseInfo {
        final byte[] data;
        final InetAddress address;
        final int port;
        final RequestType requestType;

        ResponseInfo(byte[] data, InetAddress address, int port, RequestType requestType) {
            this.data = data;
            this.address = address;
            this.port = port;
            this.requestType = requestType;
        }
    }

}

