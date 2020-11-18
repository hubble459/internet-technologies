package server;

import java.io.*;
import java.net.Socket;

public class SocketProcess implements Runnable {
    private final Socket socket;
    private final BufferedReader reader;
    private final PrintWriter writer;
    private final String ip;
    private boolean connected;
    private boolean loggedIn;
    private boolean ponged;
    private OnActionListener listener;
    private String username;

    public SocketProcess(Socket socket) throws IOException {
        this.socket = socket;
        this.ip = socket.getInetAddress().getHostAddress();
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    public void addOnActionListener(OnActionListener listener) {
        this.listener = listener;
    }

    @Override
    public void run() {
        if (!socket.isClosed()) {
            username = "unknown@" + ip;
            System.out.println("[" + username + "] Joined the server");
            connected = true;
            sendMessage(new Message(Command.INFO, "Welcome"));
        }

        while (!socket.isClosed() && connected) {
            String line;
            try {
                line = reader.readLine();
            } catch (IOException e) {
                System.err.println(e.getMessage());
                break;
            }

            if (line == null) {
                continue;
            } else {
                line = line.trim();
            }

            String[] split = line.split(" ");
            if (split.length == 0) {
                sendMessage(new Message(Command.UNKNOWN, "Unknown command"));
            } else {
                String command = split[0];
                String payload = "";
                if (split.length > 1) {
                    payload = line.substring(command.length() + 1);
                }
                Message message;
                Command cmd = Command.fromCommand(command);
                if (cmd == null) {
                    sendMessage(new Message(Command.UNKNOWN, "Unknown command"));
                    continue;
                } else {
                    message = new Message(cmd, payload);
                }

                System.out.println("[" + username + "] " + message);

                switch (message.getCommand()) {
                    case LOGIN:
                        if (payload.matches("\\w{3,14}")) {
                            username = payload + "@" + ip;
                            sendMessage(new Message(Command.LOGGED_IN, "Logged in as " + payload));
                            loggedIn = true;
                            if (listener != null) {
                                listener.connected(username);
                            }
                        } else {
                            sendMessage(new Message(Command.INVALID_FORMAT, "Name should be between 3 and 14 characters and should match [a-zA-Z_0-9]"));
                        }
                        break;
                    case QUIT:
                        sendMessage(new Message(Command.QUITED, "Quit successfully"));
                        connected = false;
                        if (listener != null) {
                            listener.disconnected();
                        }
                        break;
                    case BROADCAST:
                        if (loggedIn && listener != null) {
                            listener.broadcast(username, message);
                        }
                        break;
                    case PONG:
                        ponged = true;
                        break;
                }
            }
        }

        try {
            if (!socket.isClosed()) {
                socket.close();
            }
            reader.close();
            writer.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

        if (listener != null) {
            listener.disconnected();
        }
    }

    public void sendMessage(Message message) {
        if (!socket.isClosed()) {
            System.out.println("[SERVER -> " + username + "] " + message);
            writer.println(message.toString());
            writer.flush();
        }
    }

    public void timeout() {
        if (listener != null) {
            sendMessage(new Message(Command.DISCONNECTED, "Connection timed out"));
            listener.disconnected();
        }

        connected = false;
        if (!socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void ping() {
        ponged = false;
        sendMessage(new Message(Command.PING, ""));
    }

    public boolean hasPonged() {
        return ponged;
    }

    public interface OnActionListener {
        void disconnected();

        void connected(String username);

        void broadcast(String username, Message message);
    }
}
