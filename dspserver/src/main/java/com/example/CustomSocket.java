package com.example;

import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CustomSocket {

    private int portNumber = 0;
    private DatagramSocket socket;

    public CustomSocket(int portNumber) {
        this.socket = null;
        this.portNumber = portNumber;
    }

    public void createServer() throws Exception{
        DatagramSocket socket = new DatagramSocket(portNumber);
        this.socket = socket;
    }

    public void receive(byte[] receiveBuffer) throws Exception {
        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
        socket.receive(receivePacket);

        ObjectMapper objectMapper = new ObjectMapper();

        try {
            InputStream schemaStream = Main.class.getClassLoader().getResourceAsStream("interface.json");
            InputStream servicesStream = Main.class.getClassLoader().getResourceAsStream("services.json");

            if (schemaStream == null || servicesStream == null) {
                throw new RuntimeException("JSON files not found in resources.");
            }

            List<Map<String, Object>> schema = objectMapper.readValue(schemaStream, new TypeReference<>() {});
            List<Map<String, Object>> servicesSchema = objectMapper.readValue(servicesStream, new TypeReference<>() {});

            Parser parser = new Parser(schema, servicesSchema);

            Map<String, Object> result = parser.unmarshall(receiveBuffer);

            System.out.println("Unmarshalled data");
            result.forEach((key, value) -> System.out.println(key + ": " + value));

        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }



}
