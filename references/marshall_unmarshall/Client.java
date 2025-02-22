import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket();
        InetAddress serverAddress = InetAddress.getByName("localhost");
        int serverPort = 9877;

        // Create request object
        FacilityBookingRequest request = new FacilityBookingRequest("Alice", "Room A", "Monday 10 AM");

        // Marshalling: Convert object to byte array
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream objStream = new ObjectOutputStream(byteStream);
        objStream.writeObject(request);
        objStream.close();
        byte[] requestData = byteStream.toByteArray();

        // Send request
        DatagramPacket sendPacket = new DatagramPacket(requestData, requestData.length, serverAddress, serverPort);
        socket.send(sendPacket);
        System.out.println("Sent request: " + request);

        // Receive response
        byte[] receiveBuffer = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
        socket.receive(receivePacket);
        byte[] responseData = receivePacket.getData();

        // Unmarshalling: Convert byte array to response object
        ByteArrayInputStream byteInStream = new ByteArrayInputStream(responseData);
        ObjectInputStream objInStream = new ObjectInputStream(byteInStream);
        FacilityBookingResponse response = (FacilityBookingResponse) objInStream.readObject();
        objInStream.close();

        System.out.println("Received response: " + response);

        socket.close();
    }
}
