package unreliable;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

public class UDPServer {
    private static final int PORT = 5000;
    private static final int EXPECTED_TOTAL_PACKETS = 100;
    private static final int INACTIVITY_TIMEOUT_MS = 3000; // 3 seconds timeout

    public static void main(String[] args) throws IOException {
        DatagramSocket socket = new DatagramSocket(PORT);
        System.out.println("UDP Server listening on port " + PORT + "...");
        System.out.println("Will stop after " + (INACTIVITY_TIMEOUT_MS / 1000) + " seconds of inactivity\n");

        boolean[] receivedPackets = new boolean[EXPECTED_TOTAL_PACKETS + 1]; // Index 1-100
        int receivedPacketsCount = 0;
        AtomicLong lastPacketTime = new AtomicLong(System.currentTimeMillis());

        try {
            while (true) {
                // Set timeout for receive operation
                socket.setSoTimeout(1000); // Check every second for timeout

                try {
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    // Update last packet time
                    lastPacketTime.set(System.currentTimeMillis());

                    // Extract sequence number
                    int sequenceNumber = ByteBuffer.wrap(packet.getData()).getInt();
                    System.out.println("Received packet #" + sequenceNumber);

                    if (sequenceNumber >= 1 && sequenceNumber <= EXPECTED_TOTAL_PACKETS &&
                            !receivedPackets[sequenceNumber]) {
                        receivedPackets[sequenceNumber] = true;
                        receivedPacketsCount++;
                    }

                    // Optional: Stop if we've received all expected packets
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
            socket.close();
        }
    }

    private static void printStatistics(boolean[] receivedPackets, int receivedCount) {
        int losses = 0;

        // Identify lost packets
        for (int i = 1; i <= EXPECTED_TOTAL_PACKETS; i++) {
            if (!receivedPackets[i]) {
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