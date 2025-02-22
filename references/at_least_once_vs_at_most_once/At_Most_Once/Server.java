package At_Most_Once;

import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class Server {
    private static final int PORT = 9876;
    private static Map<String, String> requestHistory = new HashMap<>();

    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket(PORT);
        byte[] receiveBuffer = new byte[1024];

        System.out.println("UDP Server (At-Most-Once) is running...");
        
        while (true) {
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            socket.receive(receivePacket);

            String request = new String(receivePacket.getData(), 0, receivePacket.getLength());
            String[] parts = request.split(";", 2);
            String requestId = parts[0];
            String requestData = parts[1];

            System.out.println("Received request ID: " + requestId);

            String response;
            if (requestHistory.containsKey(requestId)) {
                response = requestHistory.get(requestId); // Return previous response
                System.out.println("Duplicate request detected! Resending previous response.");
            } else {
                response = "Processed: " + requestData;
                requestHistory.put(requestId, response); // Store response
            }

            byte[] responseData = response.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(
                responseData, responseData.length, receivePacket.getAddress(), receivePacket.getPort()
            );
            socket.send(sendPacket);
        }
    }

}
