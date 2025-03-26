package com.example;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of CustomSocket that guarantees at-least-once message delivery.
 * Uses acknowledgments and retransmission to ensure messages are delivered.
 */
public class AtLeastOnceSocket extends CustomSocket {
    // Store pending messages until acknowledged
    private final ConcurrentHashMap<UUID, PendingMessage> pendingMessages = new ConcurrentHashMap<>();
    
    // Configuration for retransmission
    private static final int DEFAULT_TIMEOUT_MS = 1000;
    private static final int MAX_RETRIES = 5;
    private static final int ACK_SERVICE_ID = 999; // Special service ID for acknowledgments
    
    /**
     * Creates an AtLeastOnceSocket with the specified port number.
     * 
     * @param portNumber The port number for this socket
     */
    public AtLeastOnceSocket(int portNumber) {
        super(portNumber);
    }
    
    @Override
    public void createServer() throws java.net.SocketException {
        super.createServer();
        socket.setSoTimeout(DEFAULT_TIMEOUT_MS);
    }
    
    @Override
    public void createClient() throws java.net.SocketException {
        super.createClient();
        socket.setSoTimeout(DEFAULT_TIMEOUT_MS);
    }

    @Override
    public void send(Map<String, Object> message, UUID requestId, int serviceId, 
            RequestType isRequest,  InetAddress destinationAddress, int destinationPort) 
            throws IOException {
        
        try {
            // Create message and marshal it
            Parser.Message parsedMessage = createMessage(message, serviceId, isRequest);
            byte[] data = parser.marshall(parsedMessage);
            
            // Store pending message for retransmission
            PendingMessage pendingMessage = new PendingMessage(
                    data, destinationAddress, destinationPort, System.currentTimeMillis());
            pendingMessages.put(requestId, pendingMessage);
            
            // Send the initial message
            sendDatagram(data, destinationAddress, destinationPort);
            
            // Start retransmission thread if not already running
            ensureRetransmissionThreadRunning();
            
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
                
                // Convert to the expected response format
                Map<String, Object> resultMap = new java.util.HashMap<>(message.getData());
                resultMap.put("request_id", message.getRequestId());
                resultMap.put("service_id", message.getServiceId());
                resultMap.put("request_type", message.getRequestType());
                
                return new SenderResult(packet.getAddress(), packet.getPort(), resultMap);
                
            } catch (SocketTimeoutException e) {
                // Timeout is expected for retransmission logic, just try again
                continue;
            } catch (Exception e) {
                throw new IOException("Failed to receive message", e);
            }
        }
    }
    
    /**
     * Sends an acknowledgment for a received message.
     * 
     * @param requestId The ID of the message being acknowledged
     * @param address The address to send the acknowledgment to
     * @param port The port to send the acknowledgment to
     * @throws IOException If sending fails
     */
    private void sendAcknowledgment(UUID requestId, InetAddress address, int port) throws IOException {
        try {
            // Create a simple acknowledgment message
            Map<String, Object> ackData = new java.util.HashMap<>();
            ackData.put("ack", true);
            
            Parser.Message ackMessage = new Parser.Message(
                    requestId, ACK_SERVICE_ID, RequestType.LOST, "ACK", ackData);
            
            byte[] ackBytes = parser.marshall(ackMessage);
            sendDatagram(ackBytes, address, port);
            
        } catch (Exception e) {
            throw new IOException("Failed to send acknowledgment", e);
        }
    }
    
    /**
     * Ensures that a thread is running to handle retransmissions.
     */
    private void ensureRetransmissionThreadRunning() {
        // Use a single thread to handle retransmissions
        Thread retransmitThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted() && !socket.isClosed()) {
                try {
                    retransmitPendingMessages();
                    Thread.sleep(DEFAULT_TIMEOUT_MS / 2);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    // Log exception but keep thread alive
                    e.printStackTrace();
                }
            }
        });
        
        retransmitThread.setDaemon(true);
        retransmitThread.start();
    }
    
    /**
     * Checks pending messages and retransmits if needed.
     */
    private void retransmitPendingMessages() {
        long currentTime = System.currentTimeMillis();
        
        for (Map.Entry<UUID, PendingMessage> entry : pendingMessages.entrySet()) {
            PendingMessage pending = entry.getValue();
            
            // Check if timeout has occurred
            if (currentTime - pending.lastSentTime > DEFAULT_TIMEOUT_MS) {
                if (pending.retryCount >= MAX_RETRIES) {
                    // Too many retries, remove the message
                    pendingMessages.remove(entry.getKey());
                    continue;
                }
                
                try {
                    // Retransmit the message
                    sendDatagram(pending.data, pending.address, pending.port);
                    
                    // Update state for next retry
                    pending.lastSentTime = currentTime;
                    pending.retryCount++;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Class representing a message waiting for acknowledgment.
     */
    private static class PendingMessage {
        final byte[] data;
        final InetAddress address;
        final int port;
        long lastSentTime;
        int retryCount;
        
        PendingMessage(byte[] data, InetAddress address, int port, long sentTime) {
            this.data = data;
            this.address = address;
            this.port = port;
            this.lastSentTime = sentTime;
            this.retryCount = 0;
        }
    }
}