package reliable;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;
import java.util.BitSet;

public class ReliableUDPServer {
    private static final int PORT = 5000;
    private static final int EXPECTED_TOTAL_PACKETS = 100;
    private static final int INACTIVITY_TIMEOUT_MS = 3000; // 3 seconds timeout
    private static final int ACK_PORT = 5001; // Separate port for ACKs

    public static void main(String[] args) throws IOException {
        DatagramSocket dataSocket = new DatagramSocket(PORT);
        DatagramSocket ackSocket = new DatagramSocket(ACK_PORT);
        System.out.println("Reliable UDP Server listening on port " + PORT + "...");
        System.out.println("ACK socket on port " + ACK_PORT);
        System.out.println("Will stop after " + (INACTIVITY_TIMEOUT_MS / 1000) + " seconds of inactivity\n");

        BitSet receivedPackets = new BitSet(EXPECTED_TOTAL_PACKETS + 1);
        int receivedPacketsCount = 0;
        AtomicLong lastPacketTime = new AtomicLong(System.currentTimeMillis());

        try {
            while (true) {
                dataSocket.setSoTimeout(1000);
                ackSocket.setSoTimeout(1000);

                try {
                    // Receive data packet
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    dataSocket.receive(packet);

                    // Update last packet time
                    lastPacketTime.set(System.currentTimeMillis());

                    // Extract sequence number
                    int sequenceNumber = ByteBuffer.wrap(packet.getData()).getInt();
                    System.out.println("Received packet #" + sequenceNumber);

                    // Mark packet as received
                    if (sequenceNumber >= 1 && sequenceNumber <= EXPECTED_TOTAL_PACKETS &&
                            !receivedPackets.get(sequenceNumber)) {
                        receivedPackets.set(sequenceNumber);
                        receivedPacketsCount++;

                        // Send ACK back to client
                        sendAck(ackSocket, packet.getAddress(), packet.getPort(), sequenceNumber);
                    }

                    // Stop if we've received all expected packets
                    if (receivedPacketsCount == EXPECTED_TOTAL_PACKETS) {
                        System.out.println("\nAll expected packets received!");
                        break;
                    }
                } catch (SocketTimeoutException e) {
                    // Check if we've exceeded the inactivity timeout
                    long inactiveDuration = System.currentTimeMillis() - lastPacketTime.get();
                    if (inactiveDuration >= INACTIVITY_TIMEOUT_MS) {
                        System.out.println("\nNo packets received for " + (INACTIVITY_TIMEOUT_MS / 1000)
                                + " seconds. Stopping...");
                        break;
                    }
                }
            }
        } finally {
            // Calculate and display statistics
            printStatistics(receivedPackets, receivedPacketsCount);
            dataSocket.close();
            ackSocket.close();
        }
    }

    private static void sendAck(DatagramSocket ackSocket, InetAddress clientAddress, int clientPort, int sequenceNumber)
            throws IOException {
        // Create ACK packet with sequence number
        ByteBuffer ackBuffer = ByteBuffer.allocate(4);
        ackBuffer.putInt(sequenceNumber);
        byte[] ackData = ackBuffer.array();

        DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, clientAddress, clientPort);
        ackSocket.send(ackPacket);
        System.out.println("Sent ACK for packet #" + sequenceNumber);
    }

    private static void printStatistics(BitSet receivedPackets, int receivedCount) {
        int losses = 0;

        // Identify lost packets
        for (int i = 1; i <= EXPECTED_TOTAL_PACKETS; i++) {
            if (!receivedPackets.get(i)) {
                System.out.println("Detected lost packet: #" + i);
                losses++;
            }
        }

        System.out.println("\n--- Final Results ---");
        System.out.println("Expected packets: " + EXPECTED_TOTAL_PACKETS);
        System.out.println("Received packets: " + receivedCount);
        System.out.println("Lost packets: " + losses);
        System.out.printf("Loss percentage: %.2f%%\n", (losses * 100.0 / EXPECTED_TOTAL_PACKETS));
    }
}