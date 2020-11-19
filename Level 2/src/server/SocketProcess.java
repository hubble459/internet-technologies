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

    public void setOnActionListener(OnActionListener listener) {
        this.listener = listener;
    }

    @Override
    public void run() {
        assert listener != null;

        if (!socket.isClosed()) {
            username = "unknown@" + ip;
            System.out.println("[" + username + "] Connected to the server");
            connected = true;
            sendMessage(Command.INFO, "Welcome");
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
                sendMessage(Command.UNKNOWN, "Unknown command");
            } else {
                String command = split[0];
                String payload = "";
                if (split.length > 1) {
                    payload = line.substring(command.length() + 1);
                }

                Message message;
                Command cmd = Command.fromCommand(command);
                if (cmd == null) {
                    sendMessage(Command.UNKNOWN, "Unknown command");
                    continue;
                } else {
                    message = new Message(cmd, payload);
                }

                System.out.println("[" + username + "] " + message);

                handleMessage(message);
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

        listener.disconnected();
    }

    private void handleMessage(Message message) {
        String payload = message.getPayload();

        switch (message.getCommand()) {
            case LOGIN:
                if (payload.matches("\\w{3,14}")) {
                    username = payload + "@" + ip;
                    sendMessage(Command.LOGGED_IN, "Logged in as " + payload);
                    loggedIn = true;
                    listener.connected(username);
                } else {
                    sendMessage(Command.INVALID_FORMAT, "Name should be between 3 and 14 characters and should match [a-zA-Z_0-9]");
                }
                break;
            case ROOMS:
                if (loggedIn) {
                    listener.sendRooms();
                } else {
                    notLoggedIn();
                }
                break;
            case CREATE_ROOM:
                if (loggedIn) {
                    listener.createRoom(message);
                } else {
                    notLoggedIn();
                }
                break;
            case JOIN_ROOM:
                if (loggedIn) {
                    listener.joinRoom(username, message);
                } else {
                    notLoggedIn();
                }
                break;
            case BROADCAST_IN_ROOM:
                if (loggedIn) {
                    listener.talkInRoom(username, message);
                } else {
                    notLoggedIn();
                }
                break;
            case QUIT:
                if (loggedIn) {
                    sendMessage(Command.QUITED, "Quit successfully");
                    connected = false;
                    listener.disconnected();
                } else {
                    notLoggedIn();
                }
                break;
            case BROADCAST:
                if (loggedIn) {
                    listener.broadcast(username, message);
                } else {
                    notLoggedIn();
                }
                break;
            case PONG:
                ponged = true;
                break;
        }
    }

    private void notLoggedIn() {
        sendMessage(Command.NOT_LOGGED_IN, "Please log in first");
    }

    public void sendMessage(Command command, String payload) {
        sendMessage(new Message(command, payload));
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
            sendMessage(Command.DISCONNECTED, "Connection timed out");
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

    public String getUsername() {
        return username;
    }

    public void ping() {
        ponged = false;
        sendMessage(Command.PING, "");
    }

    public boolean hasPonged() {
        return ponged;
    }

    public interface OnActionListener {
        void disconnected();

        void sendRooms();

        void joinRoom(String username, Message message);

        void voteKick(Message message);

        void createRoom(Message message);

        void leaveRoom(String username);

        void connected(String username);

        void broadcast(String username, Message message);

        void talkInRoom(String username, Message message);
    }
}
