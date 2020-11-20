package server;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class SocketProcess implements Runnable {
    private final Socket socket;
    private final BufferedReader reader;
    private final PrintWriter writer;
    private final ArrayList<SocketProcess> clients;
    private final ArrayList<Room> rooms;
    private boolean connected;
    private boolean loggedIn;
    private boolean ponged;
    private OnLoginListener onLoginListener;
    private String username;

    public SocketProcess(Socket socket, ArrayList<SocketProcess> clients, ArrayList<Room> rooms) throws IOException {
        this.socket = socket;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
        this.clients = clients;
        this.rooms = rooms;
    }

    public void setOnLoginListener(OnLoginListener onLoginListener) {
        this.onLoginListener = onLoginListener;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public void run() {
        /*
         * When connected send an INFO message welcoming the client
         * And log it in the console
         */
        if (!socket.isClosed()) {
            username = socket.getInetAddress().getHostAddress();
            System.out.println("[" + username + "] Connected to the server");
            connected = true;
            sendMessage(Command.INFO, "Welcome");
        }

        /*
         * While the socket isn't closed by either the client or the server, keep listening for input
         */
        while (!socket.isClosed() && connected) {
            String line;
            try {
                // Read line from client
                line = reader.readLine();
            } catch (IOException e) {
                System.err.println(e.getMessage());
                break;
            }

            // If the line is null the connection is probably broken
            if (line == null) {
                connected = false;
                break;
            } else {
                // Make sure there are no trailing white spaces
                line = line.trim();
            }

            // Dissect the message
            Message message = Message.fromString(line);
            if (message == null) {
                sendMessage(Command.UNKNOWN, "Unknown command");
            } else {
                System.out.println("[" + username + "] " + message);
                handleMessage(message);
            }
        }

        // If out of the while loop, you are disconnected
        disconnected();

        try {
            // Close streams if they aren't already closed
            if (!socket.isClosed()) {
                socket.close();
            }
            reader.close();
            writer.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private void handleMessage(Message message) {
        String payload = message.getPayload();

        switch (message.getCommand()) {
            case LOGIN:
                login(payload);
                break;
            case USERS:
                if (loggedIn) {
                    sendUsers();
                } else {
                    notLoggedIn();
                }
                break;
            case ROOMS:
                if (loggedIn) {
                    sendRooms();
                } else {
                    notLoggedIn();
                }
                break;
            case CREATE_ROOM:
                if (loggedIn) {
                    createRoom(message);
                } else {
                    notLoggedIn();
                }
                break;
            case JOIN_ROOM:
                if (loggedIn) {
                    joinRoom(message);
                } else {
                    notLoggedIn();
                }
                break;
            case LEAVE_ROOM:
                if (loggedIn) {
                    leaveRoom();
                } else {
                    notLoggedIn();
                }
                break;
            case BROADCAST_IN_ROOM:
                if (loggedIn) {
                    talkInRoom(message);
                } else {
                    notLoggedIn();
                }
                break;
            case QUIT:
                if (loggedIn) {
                    sendMessage(Command.QUITED, "Quit successfully");
                    connected = false;
                } else {
                    notLoggedIn();
                }
                break;
            case BROADCAST:
                if (loggedIn) {
                    broadcast(message);
                } else {
                    notLoggedIn();
                }
                break;
            case PONG:
                ponged = true;
                break;
        }
    }

    private void login(String payload) {
        if (!loggedIn) {
            if (payload.matches("\\w{3,14}")) {
                if (!usernameExists(payload)) {
                    username = payload;
                    loggedIn = true;

                    sendMessage(Command.LOGGED_IN, "Logged in as " + username);

                    Message message = new Message(Command.JOINED, username);
                    for (SocketProcess client : clients) {
                        client.sendMessage(message);
                    }

                    clients.add(this);

                    // Tell listener that we logged in
                    if (onLoginListener != null) onLoginListener.loggedIn(this);
                } else {
                    sendMessage(Command.ALREADY_LOGGED_IN, "User with username '" + payload + "' is already logged in");
                }
            } else {
                sendMessage(Command.INVALID_FORMAT, "Name should be between 3 and 14 characters and should match [a-zA-Z_0-9]");
            }
        } else {
            sendMessage(Command.ALREADY_LOGGED_IN, "Please logout first");
        }
    }

    private boolean usernameExists(String username) {
        for (SocketProcess client : clients) {
            if (client.username.equals(username)) {
                return true;
            }
        }
        return false;
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
        sendMessage(Command.DISCONNECTED, "Connection timed out");
        disconnected();

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
        sendMessage(Command.PING, "");
    }

    public boolean hasPonged() {
        return ponged;
    }

    public void disconnected() {
        broadcast(new Message(Command.LEFT, "left :("));
        clients.remove(this);
    }

    public void sendUsers() {
        StringBuilder userList = new StringBuilder();
        for (SocketProcess user : clients) {
            if (user != this) {
                userList.append(user.getUsername()).append(";");
            }
        }
        if (clients.size() > 1) {
            // Remove trailing ;
            userList.setLength(userList.length() - 1);
        }

        sendMessage(Command.USERS, userList.toString());
    }

    public void sendRooms() {
        StringBuilder roomList = new StringBuilder();
        for (Room room : rooms) {
            roomList.append(room.getRoomName()).append(";");
        }
        if (!rooms.isEmpty()) {
            // Remove trailing ;
            roomList.setLength(roomList.length() - 1);
        }

        sendMessage(Command.ROOMS, roomList.toString());
    }

    public void joinRoom(Message message) {
        String roomName = message.getPayload();
        boolean exist = false;
        Room current = null;
        if (roomName.matches("\\w{3,14}")) {
            for (Room room : rooms) {
                if (room.contains(this)) {
                    current = room;

                    if (exist) {
                        break;
                    }
                }

                if (room.getRoomName().equals(roomName)) {
                    room.join(this);
                    exist = true;

                    if (current != null) {
                        break;
                    }
                }
            }
        }
        if (!exist) {
            sendMessage(Command.UNKNOWN, "Room with name '" + roomName + "' does not exist!");
        } else if (current != null) {
            current.leave(this);
        }
    }

    public void talkInRoom(Message message) {
        message.setPayload((username + " " + message.getPayload()).trim());
        boolean inRoom = false;
        for (Room room : rooms) {
            if (room.contains(this)) {
                room.broadcast(message);
                inRoom = true;
                break;
            }
        }

        if (!inRoom) {
            sendMessage(Command.NOT_IN_A_ROOM, "You haven't joined a room to talk in");
        }
    }

    public void voteKick(Message message) {

    }

    public void createRoom(Message message) {
        String roomName = message.getPayload();
        if (!roomNameExists(roomName)) {
            if (roomName.matches("\\w{3,14}")) {
                Room room = new Room(message.getPayload());
                rooms.add(room);
                sendMessage(Command.ROOM_CREATED, room.toString());
            } else {
                sendMessage(Command.INVALID_FORMAT, "Room name should be between 3 and 14 characters, and should match [a-zA-Z_0-9]");
            }
        }else {
            sendMessage(Command.ROOM_NAME_EXIST, "Room with name " + roomName + " already exists");
        }
    }

    private boolean roomNameExists(String roomName) {
        for (Room room: rooms) {
            if (room.getRoomName().equals(roomName)) {
                return true;
            }
        }
        return false;
    }

    public void leaveRoom() {
        boolean isInRoom = false;
        for (Room room : rooms) {
            if (room.contains(this)) {
                room.leave(this);
                isInRoom = true;
                break;
            }
        }

        if (!isInRoom) {
            sendMessage(Command.NOT_IN_A_ROOM, "You're not in a room!");
        }
    }

    public void broadcast(Message message) {
        message.setPayload((username + " " + message.getPayload()).trim());
        for (SocketProcess client : clients) {
            client.sendMessage(message);
        }
    }

    public interface OnLoginListener {
        void loggedIn(SocketProcess process);
    }
}
