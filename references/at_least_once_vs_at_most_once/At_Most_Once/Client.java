package At_Most_Once;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.UUID;

public class Client {
     public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout(2000); // 2-second timeout

        InetAddress serverAddress = InetAddress.getByName("localhost");
        int serverPort = 9876;
        String requestId = UUID.randomUUID().toString();
        String requestData = "Book Room A on Monday at 10 AM";

        String request = requestId + ";" + requestData;
        byte[] requestBytes = request.getBytes();

        DatagramPacket sendPacket = new DatagramPacket(requestBytes, requestBytes.length, serverAddress, serverPort);
        socket.send(sendPacket);
        System.out.println("Sent request: " + requestData);

        byte[] receiveBuffer = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
        socket.receive(receivePacket);

        String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
        System.out.println("Received response: " + response);

        socket.close();
    }
}
