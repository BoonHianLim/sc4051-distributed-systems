import java.io.*;
import java.net.*;

public class Server {
    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket(9877);
        byte[] receiveBuffer = new byte[1024];

        System.out.println("UDP Server is running...");

        while (true) {
            // Receive packet
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            socket.receive(receivePacket);
            byte[] requestData = receivePacket.getData();

            System.out.println(requestData);

            // Unmarshalling: Convert byte array back to object
            ByteArrayInputStream byteStream = new ByteArrayInputStream(requestData);
            ObjectInputStream objStream = new ObjectInputStream(byteStream);
            FacilityBookingRequest request = (FacilityBookingRequest) objStream.readObject();
            objStream.close();

            System.out.println("Received request: " + request);

            // Process request (dummy logic)
            FacilityBookingResponse response = new FacilityBookingResponse(true, "Booking confirmed for " + request);

            // Marshalling: Convert response object to byte array
            ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
            ObjectOutputStream objOutStream = new ObjectOutputStream(byteOutStream);
            objOutStream.writeObject(response);
            objOutStream.close();

            byte[] responseData = byteOutStream.toByteArray();

            // Send response
            DatagramPacket sendPacket = new DatagramPacket(
                responseData, responseData.length, receivePacket.getAddress(), receivePacket.getPort()
            );
            socket.send(sendPacket);
        }
    }
}

