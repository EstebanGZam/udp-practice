package reliable;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

public class ReliableUDPClient {
    private static final int SERVER_DATA_PORT = 5000;
    private static final int MAX_RETRANSMISSIONS = 3;
    private static final long ACK_TIMEOUT_MS = 1000; // 1 second timeout for ACK

    public static void main(String[] args) throws IOException, InterruptedException {
        // Validate command line arguments
        if (args.length != 3) {
            System.err.println("Usage: java ReliableUDPClient <server_ip> <packet_size> <rate>");
            System.err.println("Example: java ReliableUDPClient 192.168.1.10 1024 10");
            System.exit(1);
        }

        String serverIP = args[0];
        int packetSize = Integer.parseInt(args[1]);
        int rate = Integer.parseInt(args[2]);

        // Validate packet size is at least 4 bytes (for sequence number)
        if (packetSize < 4) {
            System.err.println("Packet size must be at least 4 bytes");
            System.exit(1);
        }

        // Validate rate is positive
        if (rate <= 0) {
            System.err.println("Rate must be a positive number");
            System.exit(1);
        }

        final int TOTAL_PACKETS = 100; // Fixed number of packets

        DatagramSocket dataSocket = new DatagramSocket();
        DatagramSocket ackSocket = new DatagramSocket();
        InetAddress serverAddress = InetAddress.getByName(serverIP);

        // Set receive timeout for ACK socket
        ackSocket.setSoTimeout((int) ACK_TIMEOUT_MS);

        long interval = 1000 / rate; // Time between packets (ms)
        System.out.println("\nSending " + TOTAL_PACKETS + " packets to " + serverIP + "...");
        System.out.println("Packet size: " + packetSize + " bytes");
        System.out.println("Send rate: " + rate + " packets/second\n");

        Set<Integer> unackedPackets = new HashSet<>();

        // First pass: send all packets
        for (int i = 1; i <= TOTAL_PACKETS; i++) {
            sendPacket(dataSocket, serverAddress, SERVER_DATA_PORT, i, packetSize);
            unackedPackets.add(i);
            Thread.sleep(interval);
        }

        // Retransmission phase
        long startTime = System.currentTimeMillis();
        while (!unackedPackets.isEmpty()) {
            // Check for timeout
            if (System.currentTimeMillis() - startTime > TOTAL_PACKETS * interval * MAX_RETRANSMISSIONS) {
                System.out.println("Transmission failed after multiple retries.");
                break;
            }

            // Try to receive ACKs
            try {
                byte[] ackBuffer = new byte[4];
                DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
                ackSocket.receive(ackPacket);

                // Extract sequence number of ACKed packet
                int ackedSequence = ByteBuffer.wrap(ackPacket.getData()).getInt();
                unackedPackets.remove(ackedSequence);
                System.out.println("Received ACK for packet #" + ackedSequence);
            } catch (SocketTimeoutException e) {
                // Retransmit unacked packets
                for (int sequenceNumber : unackedPackets.toArray(new Integer[0])) {
                    System.out.println("Retransmitting packet #" + sequenceNumber);
                    sendPacket(dataSocket, serverAddress, SERVER_DATA_PORT, sequenceNumber, packetSize);
                    Thread.sleep(interval / 2); // Small delay between retransmissions
                }
            }
        }

        System.out.println("\nAll packets transmitted and acknowledged!");
        dataSocket.close();
        ackSocket.close();
    }

    private static void sendPacket(DatagramSocket socket, InetAddress serverAddress,
            int serverPort, int sequenceNumber, int packetSize) throws IOException {
        // Create packet data with sequence number and padding
        ByteBuffer buffer = ByteBuffer.allocate(packetSize);
        buffer.putInt(sequenceNumber); // First 4 bytes are the sequence number

        // Fill remaining bytes with dummy data (0s)
        while (buffer.remaining() > 0) {
            buffer.put((byte) 0);
        }

        byte[] data = buffer.array();
        DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
        socket.send(packet);
        System.out.println("Sent packet #" + sequenceNumber + " (size: " + packetSize + " bytes)");
    }
}