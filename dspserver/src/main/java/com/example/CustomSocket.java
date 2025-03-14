package com.example;

import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CustomSocket {

    private int portNumber = 0;
    private DatagramSocket socket;
    private ObjectMapper objectMapper = new ObjectMapper();
    private Parser parser;

    public CustomSocket(int portNumber) {
        this.socket = null;
        this.portNumber = portNumber;
    }

    public void createServer() throws Exception{
        DatagramSocket socket = new DatagramSocket(portNumber);
        this.socket = socket;
    }

    public void send(Map<String, Object> sendMessage, InetAddress serverAddress, int portNumber) throws Exception {

        byte[] sendBuffer = new byte[1024];

        try {
            List<Map<String, Object>> schema = loadJSONSchema("interface.json");
            List<Map<String, Object>> servicesSchema = loadJSONSchema("services.json");

            parser = new Parser(schema, servicesSchema);

            UUID requestId = (UUID) sendMessage.get("request_id");
            int serviceId = 1;
            boolean isRequest = true;
            String objName = (String) servicesSchema.get(0).get("request");

            sendBuffer = parser.marshall(requestId, serviceId, isRequest, objName, sendMessage);
            
        } catch (Exception e) {
            e.printStackTrace();
        }

        DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, serverAddress, portNumber);
        socket.send(sendPacket);
    }

    public SenderResult receive(byte[] receiveBuffer) throws Exception {

        final SenderResult senderResult;
        
        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
        socket.receive(receivePacket);

        InetAddress senderAddress = receivePacket.getAddress();
        int senderPort = receivePacket.getPort();

        try {
            List<Map<String, Object>> schema = loadJSONSchema("interface.json");
            List<Map<String, Object>> servicesSchema = loadJSONSchema("services.json");

            parser = new Parser(schema, servicesSchema);

            Map<String, Object> result = parser.unmarshall(receiveBuffer);

            senderResult = new SenderResult(senderAddress, senderPort, result);

            return senderResult;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<Map<String, Object>> loadJSONSchema(String jsonFile) throws Exception {
        InputStream jsonStream = Main.class.getClassLoader().getResourceAsStream(jsonFile);
        if (jsonStream == null) {
            throw new RuntimeException("JSON file " + jsonFile + " not found in resources");
        }
        List<Map<String, Object>> schema = objectMapper.readValue(jsonStream, new TypeReference<>() {});

        return schema;
    }
}
