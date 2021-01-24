import model.Room;
import process.SocketProcess;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * TCP Text Based ServerSocket
 * <p>
 * When run from console you could pass the port as an argument
 */
public class MyServer {
    public static final int TIMEOUT = 30000; // -1 for no timeout; in ms
    public static final int RESPONSE_TIME = 3000; // in ms
    private static final int DEFAULT_PORT = 1337;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        try {
            // Port from run arguments
            port = Integer.parseInt(args[0]);
        } catch (Exception ignored) {
        }
        try {
            new MyServer(port);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public MyServer() throws IOException {
        this(DEFAULT_PORT);
    }

    public MyServer(int port) throws IOException {
        ArrayList<SocketProcess> clients = new ArrayList<>();
        ArrayList<Room> rooms = new ArrayList<>();

        // Default rooms
        rooms.add(new Room("Random"));
        rooms.add(new Room("Waifus"));
        rooms.add(new Room("Java"));
        rooms.add(new Room("Room"));

        // Create a socket to wait for clients.
        ServerSocket serverSocket = new ServerSocket(port);

        System.out.printf("[SERVER] Started on port %d%n", serverSocket.getLocalPort());

        while (!serverSocket.isClosed()) {
            // Wait for an incoming client-connection request (blocking).
            Socket socket = serverSocket.accept();

            // Starting a processing thread for each client
            SocketProcess process = new SocketProcess(socket, clients, rooms);
            startProcess(process);

            // When client logged in
            process.setOnLoginListener(client -> {
                int size = clients.size();
                // Print the current user count
                String plural = size == 1 ? "" : "s";
                System.out.printf("[SERVER] %d client%s logged in%n", size, plural);
                // Start heartbeat
                heartbeat(client);
            });
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void heartbeat(SocketProcess client) {
        assert TIMEOUT >= -1;

        if (TIMEOUT != -1) {
            // If timeout isn't -1 start heartbeat thread for client
            new Thread(() -> {
                do {
                    // Sleep for TIMEOUT ms
                    sleep(TIMEOUT);
                    // Ping the client
                    client.ping();
                    // Give time for response
                    sleep(RESPONSE_TIME);
                    // If no pong, loop stops iterating
                } while (client.hasPonged());

                // Timed out
                client.timeout();
            }).start();
        }
    }

    private void startProcess(SocketProcess process) {
        new Thread(process).start();
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
