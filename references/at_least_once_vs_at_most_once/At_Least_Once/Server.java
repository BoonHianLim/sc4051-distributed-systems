package At_Least_Once;

import java.net.*;

public class Server {
    public static void main(String[] args) throws Exception{
        // Server listens on port 9876
        DatagramSocket socket = new DatagramSocket(9876);
        byte[] receiveBuffer = new byte[1024];

        System.out.println("UDP Server (At-Least-Once) is running...");

        while (true) {
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            socket.receive(receivePacket);

            String request = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println("Received request: " + request);

            // Simulate processing (e.g. book facility)
            String response = "Processed: " + request;

            // Send response back to client
            byte[] responseData = response.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(responseData, responseData.length, receivePacket.getAddress(), receivePacket.getPort());
            socket.send(sendPacket);
        }
    }
}
