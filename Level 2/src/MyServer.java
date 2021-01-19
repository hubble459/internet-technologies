import model.Room;
import process.SocketProcess;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class MyServer {
    public static final int TIMEOUT = 30000; // -1 for no timeout; in ms
    public static final int RESPONSE_TIME = 3000; // in ms
    private static final int DEFAULT_PORT = 1337;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        try {
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

    @SuppressWarnings("ALL")
    public MyServer(int port) throws IOException {
        ArrayList<SocketProcess> clients = new ArrayList<>();
        ArrayList<Room> rooms = new ArrayList<>();

        rooms.add(new Room("Shrek"));
        rooms.add(new Room("Waifu"));
        rooms.add(new Room("Toneel"));
        rooms.add(new Room("YoungOnes"));
        rooms.add(new Room("Fuckie"));
        rooms.add(new Room("Underaged"));
        rooms.add(new Room("Swag"));
        rooms.add(new Room("UwUs"));
        rooms.add(new Room("Hotel"));
        rooms.add(new Room("OwOs"));
        rooms.add(new Room("Emojis"));
        rooms.add(new Room("Random"));

        // Create a socket to wait for clients.
        ServerSocket serverSocket = new ServerSocket(port);

        System.out.println("[SERVER] Started on port " + serverSocket.getLocalPort());

        while (true) {
            // Wait for an incoming client-connection request (blocking).
            Socket socket = serverSocket.accept();

            // Starting a processing thread for each client
            SocketProcess process = new SocketProcess(socket, clients, rooms);
            startProcess(process);

            process.setOnLoginListener(new SocketProcess.OnLoginListener() {
                @Override
                public void loggedIn(SocketProcess process) {
                    int size = clients.size();
                    String plural = size < 0 || size == 1 ? "" : "s";
                    System.out.println("[SERVER] " + size + " client" + plural + " logged in");
                    heartbeat(process);
                }
            });
        }
    }

    @SuppressWarnings("ALL")
    private void heartbeat(SocketProcess client) {
        assert TIMEOUT >= -1;

        if (TIMEOUT != -1) {
            // If timeout isn't -1 start heartbeat thread for client
            new Thread(() -> {
                do {
                    // Don't immediately pong after login
                    // So sleep for TIMEOUT ms
                    sleep(TIMEOUT);
                    // Ping the client
                    client.ping();
                    try {
                        // Give time for response
                        Thread.sleep(RESPONSE_TIME);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
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
