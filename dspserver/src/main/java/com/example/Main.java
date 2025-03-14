package com.example;

import java.util.Map;

public class Main {
    public static void main(String[] args) throws Exception {

        final int PORT_NUMBER = 11999;
        byte[] receiveBuffer = new byte[1024];
        CustomSocket socket = new CustomSocket(PORT_NUMBER);
        socket.createServer();
        System.out.println("Server is running at " + PORT_NUMBER);

        while (true) {
            SenderResult senderResult = socket.receive(receiveBuffer);
            System.out.println("Unmarshalled data");
            Map<String, Object> result = senderResult.getResult();
            result.forEach((key, value) -> System.out.println(key + ": " + value));
            socket.send(result, senderResult.getSenderIpAddress(), senderResult.getSenderPort());
        }
    }
}
