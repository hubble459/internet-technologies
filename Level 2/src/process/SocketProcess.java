package process;

import model.Command;
import model.Message;
import model.Room;
import util.Checksum;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Locale;

public class SocketProcess implements Runnable {
    private final static String FILE_DIR = "files/";
    private final Socket socket;
    private final BufferedReader reader;
    private final PrintWriter writer;
    private final ArrayList<SocketProcess> clients;
    private final ArrayList<Room> rooms;
    private boolean connected;
    private boolean loggedIn;
    private boolean ponged;
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
                sendMessage(Command.BAD_RESPONSE, "Unknown command");
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
            case FILE:
                receiveFile(message);
                break;
            case DOWNLOAD:
                sendFile(message);
                break;
            case WHISPER:
                whisper(message);
                break;
            case VOTE_KICK:
                if (ensureLoggedIn() && ensureInRoom()) {
                    room.startKick(this);
                }
                break;
            case VOTE_KICK_USER:
                if (ensureLoggedIn() && ensureInRoom()) {
                    voteKick(message);
                }
                break;
            case VOTE_SKIP:
                if (ensureLoggedIn() && ensureInRoom()) {
                    room.voteSkip(this);
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
                    sendMessage(Command.GOOD_RESPONSE, "Quit successfully");
                    connected = false;
                }
                break;
            case BROADCAST:
                if (ensureLoggedIn() && ensureMessageGiven(message)) {
                    sendMessage(Command.GOOD_RESPONSE, message.getPayload());
                    broadcast(message);
                }
                break;
            case PONG:
                ponged = true;
                sendMessage(Command.GOOD_RESPONSE, "Pong received");
                break;
        }
    }

    /**
     * Client's Download
     * <p>
     * Server sends a file to a client
     *
     * MESSAGE: [filename]
     * RESPONSE: [base64] [checksum]
     *
     * @param message message witch contains the file, filename, the recipient and a checksum
     */
    private void sendFile(Message message) {
        String[] parts = message.getPayload().split(" ", 2);
        if (parts.length != 1) {
            sendMessage(Command.BAD_RESPONSE, "Invalid number of arguments passed");
            return;
        }
        String filename = parts[0];
        File file = new File(FILE_DIR + filename);
        if (file.exists()) {
            try {
                byte[] bytes = Files.readAllBytes(file.toPath());
                String base64 = Base64.getEncoder().encodeToString(bytes);

                sendMessage(Command.GOOD_RESPONSE, base64 + ' ' + Checksum.getMD5Checksum(file));
            } catch (Exception e) {
                System.err.println(e.getMessage());
                sendMessage(Command.BAD_RESPONSE, e.getMessage());
            }
        } else {
            sendMessage(Command.BAD_RESPONSE, String.format("Request file '%s' not found", filename));
        }
    }

    /**
     * Client's Upload
     * <p>
     * Server received a file from a client
     *
     * MESSAGE: [recipient] [filename] [file_in_base64] [checksum]
     *
     * @param message message witch contains the file, filename, the recipient and a checksum
     */
    private void receiveFile(Message message) {
        String[] parts = message.getPayload().split(" ");
        if (parts.length != 4) {
            sendMessage(Command.BAD_RESPONSE, "Invalid number of arguments passed");
            return;
        }

        String toUsername = parts[0];

        SocketProcess to = getUserFromUsername(toUsername);
        if (to != null) {
            String filename = parts[1];
            byte[] bytes = Base64.getDecoder().decode(parts[2]);
            String checksum = parts[3];

            try {
                writeFile(filename, bytes, checksum);
                double fileSize = bytes.length / 1024. / 1024.;
                String amountMegabytes = String.format(Locale.US, "%.4f", fileSize);
                sendMessage(Command.GOOD_RESPONSE, "File uploaded");
                to.sendMessage(Command.FILE, username + ' ' + filename + ' ' + amountMegabytes);
            } catch (Exception e) {
                sendMessage(Command.BAD_RESPONSE, e.getMessage());
                System.err.println(e.getMessage());
            }
        } else {
            sendMessage(Command.BAD_RESPONSE, "User with username '" + toUsername + "' does not exist");
        }
    }

    private void writeFile(String filename, byte[] bytes, String checksum) throws Exception {
        File downloads = new File(FILE_DIR);
        if (!downloads.exists() && !downloads.mkdir()) {
            throw new Exception(String.format("Error trying to create the '%s' directory", FILE_DIR));
        }

        File file = new File(FILE_DIR + filename);
        int count = 0;
        while (file.exists()) {
            file = new File(FILE_DIR + count++ + '_' + filename);
        }
        boolean created = file.createNewFile();
        if (created) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            FileWriter writer = new FileWriter(file);
            for (byte b : bytes) {
                md.update(b);
                writer.write(b);
            }
            writer.close();

            String fileChecksum = Checksum.bytesToString(md.digest());
            if (!checksum.equals(fileChecksum)) {
                throw new Exception("Uploaded file does not match given checksum");
            }
        } else {
            throw new Exception("File not uploaded");
        }
    }

    private boolean ensureMessageGiven(Message message) {
        if (message.getPayload().isEmpty()) {
            sendMessage(Command.BAD_RESPONSE, "Please give a message to send");
            return false;
        } else {
            return true;
        }
    }

    private boolean ensureLoggedIn() {
        if (loggedIn) {
            return true;
        } else {
            sendMessage(Command.BAD_RESPONSE, "Please log in first");
            return false;
        }
    }

    private boolean ensureInRoom() {
        if (room != null) {
            return true;
        } else {
            sendMessage(Command.BAD_RESPONSE, "Join a room first");
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


    private void voteKick(Message message) {
        SocketProcess user = getUserFromUsername(message.getPayload());
        if (user != null) {
            room.voteFor(this, user);
        } else {
            sendMessage(Command.BAD_RESPONSE, "No user with this username found");
        }
    }

    private void whisper(Message message) {
        String payload = message.getPayload();
        if (ensureLoggedIn()) {
            String[] split = payload.split(" ");
            if (payload.isEmpty() || split.length <= 1) {
                // Empty line is an unknown command
                sendMessage(Command.BAD_RESPONSE, "No username/message given");
            } else {
                String username = split[0];
                String msg = payload.substring(username.length() + 1);

                privateMessage(username, msg);
            }
        }
    }

    private void privateMessage(String username, String message) {
        SocketProcess user = getUserFromUsername(username);
        if (user != null) {
            Message msg = new Message(Command.WHISPER, this.username + " " + message);
            user.sendMessage(msg);

            sendMessage(Command.GOOD_RESPONSE, message);
        } else {
            sendMessage(Command.BAD_RESPONSE, "No user with this username found");
        }
    }

    private void login(String payload) {
        if (!loggedIn) {
            if (payload.matches("\\w{3,14}")) {
                if (!usernameExists(payload)) {
                    username = payload;
                    loggedIn = true;

                    sendMessage(Command.GOOD_RESPONSE, "Logged in as " + username);

                    Message message = new Message(Command.JOINED_SERVER, username + " joined the server");
                    for (SocketProcess client : clients) {
                        client.sendMessage(message);
                    }

                    clients.add(this);

                    // Tell listener that we logged in
                    if (onLoginListener != null) onLoginListener.loggedIn(this);
                } else {
                    sendMessage(Command.BAD_RESPONSE, "User with username '" + payload + "' is already logged in");
                }
            } else {
                sendMessage(Command.BAD_RESPONSE, "Name should be between 3 and 14 characters and should match [a-zA-Z_0-9]");
            }
        } else {
            sendMessage(Command.BAD_RESPONSE, "Please logout first");
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

        sendMessage(Command.GOOD_RESPONSE, userList.toString());
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

        sendMessage(Command.GOOD_RESPONSE, roomList.toString());
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

        sendMessage(Command.GOOD_RESPONSE, usersList.toString());
    }

    public void joinRoom(Message message) {
        String roomName = message.getPayload();
        boolean exist = false;
        Room current = room;
        if (roomName.matches("\\w{3,14}")) {
            for (Room room : rooms) {
                if (room.getRoomName().equals(roomName)) {
                    exist = true;

                    if (current != null) {
                        if (current == room) {
                            sendMessage(Command.BAD_RESPONSE, "You're already in this room");
                            return;
                        } else {
                            current.leave(this);
                        }
                    }

                    this.room = room;
                    room.join(this);
                }
            }
        }
        if (!exist) {
            sendMessage(Command.BAD_RESPONSE, "Room with name '" + roomName + "' does not exist!");
        }
    }

    public void talkInRoom(Message message) {
        message.setPayload((username + " " + message.getPayload()).trim());
        boolean inRoom = false;
        for (Room room : rooms) {
            if (room.contains(this)) {
                sendMessage(Command.GOOD_RESPONSE, message.getPayload());
                room.broadcastInRoom(message);
                inRoom = true;
                break;
            }
        }

        if (!inRoom) {
            sendMessage(Command.BAD_RESPONSE, "You haven't joined a room to talk in");
        }
    }

    public void createRoom(Message message) {
        String roomName = message.getPayload();
        if (!roomNameExists(roomName)) {
            if (roomName.matches("\\w{3,14}")) {
                Room room = new Room(message.getPayload());
                rooms.add(room);
                sendMessage(Command.GOOD_RESPONSE, room.toString());
                Message roomCreated = new Message(Command.ROOM_CREATED, room.toString());
                for (SocketProcess client : clients) {
                    client.sendMessage(roomCreated);
                }
            } else {
                sendMessage(Command.BAD_RESPONSE, "Room name should be between 3 and 14 characters, and should match [a-zA-Z_0-9]");
            }
        } else {
            sendMessage(Command.BAD_RESPONSE, "Room with name " + roomName + " already exists");
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
                sendMessage(Command.GOOD_RESPONSE, "Left room");
                isInRoom = true;
                break;
            }
        }

        if (!isInRoom) {
            sendMessage(Command.BAD_RESPONSE, "You're not in a room!");
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
