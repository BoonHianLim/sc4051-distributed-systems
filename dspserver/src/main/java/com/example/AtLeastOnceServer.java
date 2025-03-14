// Use single-threaded server only

package com.example;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class AtLeastOnceServer {
    private static final int SERVER_PORT = 2222;
    private static final int BUFFER_SIZE = 1024;

    // queue to store incoming requests
    private static final BlockingQueue<ClientRequest> requestQueue = new LinkedBlockingQueue<>();

    // List of watchers (clients who subscribed)
    private static final List<Watcher> watchers = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        DatagramSocket serverSocket = new DatagramSocket(SERVER_PORT);
        System.out.println("Java Server running on port " + SERVER_PORT);

        // Start a background thread to process requests from the queue
        Thread processingThread = new Thread(() -> processRequests(serverSocket));
        processingThread.setDaemon(true);
        processingThread.start();

        // Continuously receive requests and put them in the queue
        while (true) {
            byte[] buffer = new byte[BUFFER_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            serverSocket.receive(packet);

            InetAddress clientAddress = packet.getAddress();
            int clientPort = packet.getPort();
            byte[] requestData = packet.getData();

            requestQueue.put(new ClientRequest(clientAddress, clientPort, requestData));
        }
    }

    private static void processRequests(DatagramSocket serverSocket) {
        while (true) {
            try {
                ClientRequest req = requestQueue.take();
                ByteBuffer buffer = ByteBuffer.wrap(req.data);

                // Suppose first byte is op_code
                byte opCode = buffer.get();
                long msb = buffer.getLong(); // high bits of UUID
                long lsb = buffer.getLong(); // low bits of UUID

                if (opCode == 1) {
                    // Normal request
                    int field1 = buffer.getInt();
                    int field2 = buffer.getInt();
                    int field3 = buffer.getInt();

                    long requestId = (msb << 64) | (lsb & 0xFFFFFFFFFFFFFFFFL);
                    System.out.printf("[Server] Received normal request: %016x, fields=(%d,%d,%d)\n",
                            requestId, field1, field2, field3);

                    Thread.sleep(1000);

                    // send response: just echo the UUID
                    ByteBuffer response = ByteBuffer.allocate(16);
                    response.putLong(msb);
                    response.putLong(lsb);

                    DatagramPacket respPacket = new DatagramPacket(
                            response.array(), 16,
                            req.clientAddress, req.clientPort);
                    serverSocket.send(respPacket);
                    System.out.printf("[Server] Response sent for %016x\n", requestId);

                    // *** Callback Demo ***
                    // Let's say every normal request triggers a "facility update" callback
                    broadcastCallback("Facility updated after request " + Long.toHexString(requestId), serverSocket);

                } else if (opCode == 2) {
                    // Subscribe (monitor) request
                    int monitorInterval = buffer.getInt();
                    long requestId = (msb << 64) | (lsb & 0xFFFFFFFFFFFFFFFFL);
                    System.out.printf("[Server] Received subscribe request: %016x, interval=%d\n",
                            requestId, monitorInterval);

                    // Store client in watchers
                    synchronized (watchers) {
                        watchers.add(new Watcher(req.clientAddress, req.clientPort));
                    }
                    System.out.println("[Server] Added watcher: " + req.clientAddress + ":" + req.clientPort);

                    // (Optional) we could send an immediate ack, or do nothing
                    // For simplicity, do nothing
                } else {
                    System.out.println("[Server] Unknown op_code: " + opCode);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Send a "callback" message to all watchers
    private static void broadcastCallback(String message, DatagramSocket socket) {
        byte[] data = message.getBytes();
        synchronized (watchers) {
            for (Watcher w : watchers) {
                try {
                    DatagramPacket packet = new DatagramPacket(
                            data, data.length,
                            w.address, w.port);
                    socket.send(packet);
                    System.out.println("[Server] Callback sent to " + w.address + ":" + w.port);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // A simple record to store request info
    private static class ClientRequest {
        InetAddress clientAddress;
        int clientPort;
        byte[] data;

        public ClientRequest(InetAddress clientAddress, int clientPort, byte[] data) {
            this.clientAddress = clientAddress;
            this.clientPort = clientPort;
            this.data = data;
        }
    }

    // A record for watchers (subscribed clients)
    private static class Watcher {
        InetAddress address;
        int port;

        public Watcher(InetAddress address, int port) {
            this.address = address;
            this.port = port;
        }
    }
}
