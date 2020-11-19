package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class MyServer {
    public static final int TIMEOUT = 5000; // -1 for no timeout, max 3.6e6 ms
    private static final int DEFAULT_PORT = 1337;
    private final ArrayList<SocketProcess> clients;

    public MyServer() throws IOException {
        this(DEFAULT_PORT);
    }

    @SuppressWarnings("ALL")
    public MyServer(int port) throws IOException {
        clients = new ArrayList<>();

        // Create a socket to wait for clients.
        ServerSocket serverSocket = new ServerSocket(port);

        System.out.println("[SERVER] Started");

        while (true) {
            // Wait for an incoming client-connection request (blocking).
            Socket socket = serverSocket.accept();

            // Starting a processing thread for each client
            SocketProcess process = new SocketProcess(socket);
            process.setOnActionListener(new SocketProcess.OnActionListener() {
                @Override
                public void disconnected() {
                    clients.remove(process);
                }

                @Override
                public void connected(String username) {
                    Message message = new Message(Command.JOINED, username);
                    for (SocketProcess client : clients) {
                        client.sendMessage(message);
                    }

                    heartbeat(process);

                    clients.add(process);
                }

                @Override
                public void broadcast(String username, Message message) {
                    message.setPayload((username + " " + message.getPayload()).trim());
                    for (SocketProcess client : clients) {
                        client.sendMessage(message);
                    }
                }
            });
            startProcess(process);
        }
    }

    @SuppressWarnings("ALL")
    private void heartbeat(SocketProcess client) {
        assert TIMEOUT >= -1;
        assert TIMEOUT <= 3.6e6;

        if (TIMEOUT != -1) {
            new Thread(() -> {
                do {
                    client.ping();
                    try {
                        Thread.sleep(TIMEOUT);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } while (client.hasPonged());

                client.timeout();
            }).start();
        }
    }

    private void startProcess(SocketProcess process) {
        new Thread(process).start();
    }
}
