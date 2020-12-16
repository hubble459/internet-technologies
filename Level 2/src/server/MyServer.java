package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class MyServer {
    public static final int TIMEOUT = 5000; //5000; // -1 for no timeout, max 3.6e6 ms
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


        rooms.add(new Room("owo"));
        rooms.add(new Room("swag"));
        rooms.add(new Room("shrek"));

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
        assert TIMEOUT <= 3.6e6;

        if (TIMEOUT != -1) {
            // If timeout isn't -1 start heartbeat thread for client
            new Thread(() -> {
                try {
                    // Don't immediately pong after login
                    // So sleep for TIMEOUT ms
                    Thread.sleep(TIMEOUT);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                do {
                    // Ping the client
                    client.ping();
                    try {
                        // Give time for response
                        Thread.sleep(TIMEOUT);
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
}
