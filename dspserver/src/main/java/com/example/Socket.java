package com.example;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/*
Interface for different socket implementations
public abstract class Socket {
    public abstract Object send(Object message, int serviceId, boolean isRequest);

    // Listen for data
    public abstract void listen();

    // Close the socket
    public abstract void close();
}
*/

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Socket
{
    public static void main(String[] args) {
        int port = 12000;  // Port number to listen on

        try (DatagramSocket serverSocket = new DatagramSocket(port)) {
            System.out.println("Server is listening on port " + port);

            byte[] buffer = new byte[1024];

            while (true) {
                // Create a DatagramPacket to receive incoming data
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                serverSocket.receive(packet);  // blocking call

                // Convert the packet data to a string
                String message = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Received packet from " +
                        packet.getAddress().getHostAddress() + ":" + packet.getPort() +
                        " with message: " + message);
            }
        } catch (Exception e) {
            System.err.println("Error in UDP server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

 