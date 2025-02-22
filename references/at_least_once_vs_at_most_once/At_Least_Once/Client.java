package At_Least_Once;

import java.net.*;
import java.nio.charset.StandardCharsets;

public class Client {
    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout(2000); // 2 second timeout for response

        InetAddress serverAddress = InetAddress.getLocalHost();
        int serverPort = 9876;
        String request = "Book Room A on Monday at 10AM";

        byte[] requestData = request.getBytes(StandardCharsets.UTF_8);
        DatagramPacket sendPacket = new DatagramPacket(requestData, requestData.length, serverAddress, serverPort);

        byte[] receiveBuffer = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);

        boolean receivedResponse = false;
        int retries = 3;

        while (!receivedResponse && retries > 0) {
            socket.send(sendPacket);
            System.out.println("Sent request: " + request);

            try {
                socket.receive(receivePacket); // Wait for response
                String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
                System.out.println("Received response: " + response);
                receivedResponse = true;
            } catch (SocketTimeoutException e) {
                System.out.println("Timeout! Retrying...");
                retries--;
            }
        }

        if (!receivedResponse) {
            System.out.println("Failed to receive response after retries.");
        }

        socket.close();

    }
}
