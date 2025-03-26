package com.example;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Abstract class for implementing different socket delivery guarantees.
 */
public abstract class CustomSocket implements AutoCloseable {
    protected int portNumber;
    protected DatagramSocket socket;
    protected ObjectMapper objectMapper;
    protected Parser parser;

    // Maximum UDP packet size
    protected static final int MAX_PACKET_SIZE = 65507;

    /**
     * Creates a CustomSocket with the specified port number.
     * 
     * @param portNumber The port number for this socket
     */
    public CustomSocket(int portNumber) {
        this.portNumber = portNumber;
        this.objectMapper = new ObjectMapper();
        initializeParser();
    }

    /**
     * Initializes the parser using schema files.
     */
    protected void initializeParser() {
        try {
            List<Map<String, Object>> schema = loadJSONSchema("interface.json");
            List<Map<String, Object>> servicesSchema = loadJSONSchema("services.json");
            this.parser = new Parser(schema, servicesSchema);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize parser", e);
        }
    }

    /**
     * Creates a server socket bound to the configured port.
     * 
     * @throws SocketException If the socket cannot be created
     */
    public void createServer() throws SocketException {
        this.socket = new DatagramSocket(portNumber);
    }

    /**
     * Creates a client socket (not bound to a specific port).
     * 
     * @throws SocketException If the socket cannot be created
     */
    public void createClient() throws SocketException {
        this.socket = new DatagramSocket();
    }

    /**
     * Sets a timeout for socket operations.
     * 
     * @param timeoutMillis Timeout in milliseconds
     * @throws SocketException If the timeout cannot be set
     */
    public void setTimeout(int timeoutMillis) throws SocketException {
        if (socket != null) {
            socket.setSoTimeout(timeoutMillis);
        }
    }

    /**
     * Sends a message using the socket with delivery guarantees
     * implemented by concrete subclasses.
     * 
     * @param message            The message to send
     * @param destinationAddress The destination address
     * @param destinationPort    The destination port
     * @throws IOException If an I/O error occurs
     */
    public abstract void send(Map<String, Object> message, UUID requestId, int serviceId,
            RequestType isRequest, InetAddress destinationAddress, int destinationPort)
            throws IOException;

    /**
     * Receives a message using the socket with delivery guarantees
     * implemented by concrete subclasses.
     * 
     * @return SenderResult containing the received message and sender information
     * @throws IOException If an I/O error occurs
     */
    public abstract SenderResult receive() throws IOException;

    /**
     * Low-level method to send a datagram packet.
     * 
     * @param data    The data to send
     * @param address The destination address
     * @param port    The destination port
     * @throws IOException If an I/O error occurs
     */
    protected void sendDatagram(byte[] data, InetAddress address, int port) throws IOException {
        if (socket == null) {
            throw new IllegalStateException("Socket not initialized. Call createServer() or createClient() first.");
        }

        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        socket.send(packet);
    }

    /**
     * Low-level method to receive a datagram packet.
     * 
     * @return The received datagram packet
     * @throws IOException If an I/O error occurs
     */
    protected DatagramPacket receiveDatagram() throws IOException {
        if (socket == null) {
            throw new IllegalStateException("Socket not initialized. Call createServer() or createClient() first.");
        }

        byte[] buffer = new byte[MAX_PACKET_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        return packet;
    }

    /**
     * Loads a JSON schema from resources.
     * 
     * @param jsonFile The name of the JSON file
     * @return The parsed schema
     * @throws Exception If the file cannot be read or parsed
     */
    protected List<Map<String, Object>> loadJSONSchema(String jsonFile) throws Exception {
        InputStream jsonStream = getClass().getClassLoader().getResourceAsStream(jsonFile);
        if (jsonStream == null) {
            throw new RuntimeException("JSON file " + jsonFile + " not found in resources");
        }
        return objectMapper.readValue(jsonStream, new TypeReference<>() {
        });
    }

    /**
     * Creates a message from a map for marshalling.
     * 
     * @param data      The message data
     * @param serviceId The service ID
     * @param isRequest Whether this is a request or response
     * @return A Parser.Message object ready for marshalling
     * @throws Exception If the service ID is not found or the format is invalid
     */
    protected Parser.Message createMessage(Map<String, Object> data, int serviceId, RequestType requestType)
            throws Exception {
        List<Map<String, Object>> servicesSchema = loadJSONSchema("services.json");

        String formatName = null;
        if (requestType == RequestType.REQUEST || requestType == RequestType.RESPONSE) {
            for (Map<String, Object> service : servicesSchema) {
                if (((Integer) service.get("id")).equals(serviceId)) {
                    formatName = (String) (requestType == RequestType.REQUEST ? service.get("request")
                            : service.get("response"));
                    break;
                }
            }
        } else if (requestType == RequestType.ERROR) {
            formatName = "error";
        }

        if (formatName == null) {
            throw new IllegalArgumentException("Service ID not found: " + serviceId);
        }

        UUID requestId = (UUID) data.getOrDefault("request_id", UUID.randomUUID());

        // Remove metadata fields that aren't part of the actual payload
        Map<String, Object> payloadData = new java.util.HashMap<>(data);
        payloadData.remove("request_id");
        payloadData.remove("service_id");
        payloadData.remove("is_request");

        return new Parser.Message(requestId, serviceId, requestType, formatName, payloadData);
    }

    @Override
    public void close() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}