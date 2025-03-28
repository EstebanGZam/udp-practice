package unreliable;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;

public class UDPClient {
    public static void main(String[] args) throws IOException, InterruptedException {
        // Validate command line arguments
        if (args.length != 3) {
            System.err.println("Usage: java UDPClient <serverIP> <packetSize> <rate>");
            System.err.println("Example: java UDPClient 192.168.1.10 1024 10");
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

        final int SERVER_PORT = 5000;
        final int TOTAL_PACKETS = 100; // Fixed number of packets

        DatagramSocket socket = new DatagramSocket();
        InetAddress serverAddress = InetAddress.getByName(serverIP);

        long interval = 1000 / rate; // Time between packets (ms)
        System.out.println("\nSending " + TOTAL_PACKETS + " packets to " + serverIP + "...");
        System.out.println("Packet size: " + packetSize + " bytes");
        System.out.println("Send rate: " + rate + " packets/second\n");

        for (int i = 1; i <= TOTAL_PACKETS; i++) {
            // Create packet data with sequence number and padding
            ByteBuffer buffer = ByteBuffer.allocate(packetSize);
            buffer.putInt(i); // First 4 bytes are the sequence number

            // Fill remaining bytes with dummy data (0s)
            while (buffer.remaining() > 0) {
                buffer.put((byte) 0);
            }

            byte[] data = buffer.array();
            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, SERVER_PORT);
            socket.send(packet);
            System.out.println("Sent packet #" + i + " (size: " + packetSize + " bytes)");

            Thread.sleep(interval); // Control the send rate
        }

        System.out.println("\nAll packets sent!");
        socket.close();
    }
}