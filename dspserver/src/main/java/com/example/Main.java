package com.example;

public class Main {
    public static void main(String[] args) throws Exception {

        final int PORT_NUMBER = 11999;
        byte[] receiveBuffer = new byte[1024];
        CustomSocket socket = new CustomSocket(PORT_NUMBER);
        socket.createServer();
        System.out.println("Server is running at " + PORT_NUMBER);

        while (true) {
            socket.receive(receiveBuffer);
        }
    }
}
