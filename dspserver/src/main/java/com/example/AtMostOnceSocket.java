package com.example;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Implementation of CustomSocket that guarantees at-most-once message delivery.
 * Tracks received message IDs to detect and discard duplicates.
 */
public class AtMostOnceSocket extends CustomSocket {
    // Track received message IDs to detect duplicates
    private final Set<UUID> processedMessageIds = new HashSet<>();
    
    // Maximum size of the processed message set before purging old entries
    private static final int MAX_PROCESSED_IDS = 10000;
    
    /**
     * Creates an AtMostOnceSocket with the specified port number.
     * 
     * @param portNumber The port number for this socket
     */
    public AtMostOnceSocket(int portNumber) {
        super(portNumber);
    }

    @Override
    public void send(Map<String, Object> message, UUID requestId, int serviceId, boolean isRequest, InetAddress destinationAddress, int destinationPort) 
            throws IOException {        
        try {
            // Create message and marshal it
            Parser.Message parsedMessage = createMessage(message, serviceId, requestId, isRequest);
            byte[] data = parser.marshall(parsedMessage);
            
            // Send message just once, no retransmission
            sendDatagram(data, destinationAddress, destinationPort);
            
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
                
                // Check if we've already processed this message
                synchronized (processedMessageIds) {
                    if (processedMessageIds.contains(message.getRequestId())) {
                        // Duplicate message, ignore and keep waiting for new messages
                        continue;
                    }
                    
                    // Add to processed set and manage set size
                    processedMessageIds.add(message.getRequestId());
                    if (processedMessageIds.size() > MAX_PROCESSED_IDS) {
                        pruneProcessedIds();
                    }
                }
                
                // Convert to the expected response format
                Map<String, Object> resultMap = new java.util.HashMap<>(message.getData());
                resultMap.put("request_id", message.getRequestId());
                resultMap.put("service_id", message.getServiceId());
                resultMap.put("is_request", message.isRequest());
                
                return new SenderResult(packet.getAddress(), packet.getPort(), resultMap);
                
            } catch (Exception e) {
                throw new IOException("Failed to receive message", e);
            }
        }
    }
    
    /**
     * Prunes the set of processed message IDs when it gets too large.
     * This is a simple approach - in production you might want something more sophisticated.
     */
    private void pruneProcessedIds() {
        // Simple approach: just clear half the entries when we reach the limit
        // A more sophisticated approach would use a time-based expiration strategy
        synchronized (processedMessageIds) {
            if (processedMessageIds.size() <= MAX_PROCESSED_IDS) {
                return;
            }
            
            // Create a new set with half the elements (newest ones)
            Set<UUID> newSet = new HashSet<>();
            processedMessageIds.stream()
                .skip(processedMessageIds.size() / 2)
                .forEach(newSet::add);
            
            processedMessageIds.clear();
            processedMessageIds.addAll(newSet);
        }
    }
}