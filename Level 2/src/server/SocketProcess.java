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
    private boolean voted;
    private Room room;
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
            case WHISPER:
                if (ensureLoggedIn()) {
                    String[] split = payload.split(" ");
                    if (payload.isEmpty() || split.length <= 1) {
                        // Empty line is an unknown command
                        sendMessage(Command.NO_MESSAGE, "No username/message given");
                    } else {
                        String username = split[0];
                        String msg = payload.substring(username.length() + 1);

                        privateMessage(username, msg);
                    }
                }
                break;
            case VOTE_KICK:
                if (ensureLoggedIn() && ensureInRoom()) {
                    room.startKick();
                }
                break;
            case VOTE_KICK_USER:
                if (ensureLoggedIn() && ensureInRoom() && ensureNotVoted()) {
                    SocketProcess user = getUserFromUsername(payload);
                    if (user != null) {
                        voted = true;
                        room.voteFor(user);
                    } else {
                        sendMessage(Command.UNKNOWN, "No user with this username found");
                    }
                }
                break;
            case VOTE_SKIP:
                if (ensureLoggedIn() && ensureInRoom() && ensureNotVoted()) {
                    voted = true;
                    room.voteSkip();
                }
                break;
            case LOGIN:
                login(payload);
                break;
            case USERS:
                if (ensureLoggedIn()) {
                    sendUsers();
                }
                break;
            case ROOMS:
                if (ensureLoggedIn()) {
                    sendRooms();
                }
                break;
            case ROOM:
                if (ensureLoggedIn() && ensureInRoom()) {
                    sendUsersInRoom();
                }
                break;
            case CREATE_ROOM:
                if (ensureLoggedIn()) {
                    createRoom(message);
                }
                break;
            case JOIN_ROOM:
                if (ensureLoggedIn()) {
                    joinRoom(message);
                }
                break;
            case LEAVE_ROOM:
                if (ensureLoggedIn()) {
                    leaveRoom();
                }
                break;
            case BROADCAST_IN_ROOM:
                if (ensureLoggedIn() && ensureMessageGiven(message)) {
                    talkInRoom(message);
                }
                break;
            case QUIT:
                if (ensureLoggedIn()) {
                    sendMessage(Command.QUITED, "Quit successfully");
                    connected = false;
                }
                break;
            case BROADCAST:
                if (ensureLoggedIn() && ensureMessageGiven(message)) {
                    broadcast(message);
                }
                break;
            case PONG:
                ponged = true;
                break;
        }
    }

    private boolean ensureNotVoted() {
        if (voted) {
            sendMessage(Command.ALREADY_VOTED, "You have already voted");
            return false;
        } else {
            return true;
        }
    }

    public void setVoted(boolean voted) {
        this.voted = voted;
    }

    private boolean ensureMessageGiven(Message message) {
        if (message.getPayload().isEmpty()) {
            sendMessage(Command.NO_MESSAGE, "Please give a message to send");
            return false;
        } else {
            return true;
        }
    }

    private boolean ensureLoggedIn() {
        if (loggedIn) {
            return true;
        } else {
            sendMessage(Command.NOT_LOGGED_IN, "Please log in first");
            return false;
        }
    }

    private boolean ensureInRoom() {
        if (room != null) {
            return true;
        } else {
            sendMessage(Command.NOT_IN_A_ROOM, "Join a room first");
            return false;
        }
    }

    private SocketProcess getUserFromUsername(String username) {
        for (SocketProcess client : clients) {
            if (client.getUsername().equals(username)) {
                return client;
            }
        }
        return null;
    }

    private void privateMessage(String username, String message) {
        SocketProcess user = getUserFromUsername(username);
        if (user != null) {
            Message msg = new Message(Command.WHISPER, this.username + " " + message);
            user.sendMessage(msg);
            sendMessage(msg);
        } else {
            sendMessage(Command.UNKNOWN, "No user with this username found");
        }
    }

    private void login(String payload) {
        if (!loggedIn) {
            if (payload.matches("\\w{3,14}")) {
                if (!usernameExists(payload)) {
                    username = payload;
                    loggedIn = true;

                    sendMessage(Command.LOGGED_IN, "Logged in as " + username);

                    Message message = new Message(Command.JOINED_SERVER, username + " joined the server");
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
        broadcast(new Message(Command.LEFT, "left"));
        if (room != null) {
            room.leave(this);
        }
        clients.remove(this);
        connected = false;
    }

    public void sendUsers() {
        StringBuilder userList = new StringBuilder();
        for (SocketProcess user : clients) {
            userList.append(user.getUsername()).append(";");
        }
        if (!clients.isEmpty()) {
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

    public void sendUsersInRoom() {
        if (room == null) return;

        StringBuilder usersList = new StringBuilder();
        for (SocketProcess client : room.getClients()) {
            usersList.append(client.getUsername()).append(";");
        }
        if (usersList.length() > 0) {
            usersList.deleteCharAt(usersList.length() - 1);
        }

        sendMessage(Command.ROOM, usersList.toString());
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
                    this.room = room;

                    exist = true;

                    if (current != null) {
                        break;
                    }
                }
            }
        }
        if (!exist) {
            sendMessage(Command.UNKNOWN, "Room with name '" + roomName + "' does not exist!");
        } else if (current == room) {
            sendMessage(Command.ALREADY_IN_ROOM, "You're already in this room");
            return;
        }

        if (exist) {
            room.join(this);
            if (current != null) {
                current.leave(this);
            }
        }
    }

    public void talkInRoom(Message message) {
        message.setPayload((username + " " + message.getPayload()).trim());
        boolean inRoom = false;
        for (Room room : rooms) {
            if (room.contains(this)) {
                room.broadcastInRoom(message);
                inRoom = true;
                break;
            }
        }

        if (!inRoom) {
            sendMessage(Command.NOT_IN_A_ROOM, "You haven't joined a room to talk in");
        }
    }

    public void createRoom(Message message) {
        String roomName = message.getPayload();
        if (!roomNameExists(roomName)) {
            if (roomName.matches("\\w{3,14}")) {
                Room room = new Room(message.getPayload());
                rooms.add(room);
                Message roomCreated = new Message(Command.ROOM_CREATED, room.toString());
                for (SocketProcess client : clients) {
                    client.sendMessage(roomCreated);
                }
            } else {
                sendMessage(Command.INVALID_FORMAT, "Room name should be between 3 and 14 characters, and should match [a-zA-Z_0-9]");
            }
        } else {
            sendMessage(Command.ROOM_NAME_EXIST, "Room with name " + roomName + " already exists");
        }
    }

    private boolean roomNameExists(String roomName) {
        for (Room room : rooms) {
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
                this.room = null;
                room.leave(this);
                isInRoom = true;
                break;
            }
        }

        if (!isInRoom) {
            sendMessage(Command.NOT_IN_A_ROOM, "You're not in a room!");
        }
    }

    public void clearRoom() {
        this.room = null;
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
