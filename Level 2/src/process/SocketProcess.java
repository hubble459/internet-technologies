package process;

import model.Command;
import model.Message;
import model.Room;
import util.Checksum;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
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
    private PublicKey publicKey;

    /**
     * Create a SocketProcess Object
     * <p>
     * Handles sending commands to a client and receiving them
     *
     * @param socket  Socket to handle
     * @param clients list of all client
     * @param rooms   list of all rooms
     * @throws IOException if connection could not be made
     */
    public SocketProcess(Socket socket, ArrayList<SocketProcess> clients, ArrayList<Room> rooms) throws IOException {
        this.socket = socket;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
        this.clients = clients;
        this.rooms = rooms;
    }

    /**
     * Set a listener for the login
     *
     * @param onLoginListener loginListener
     */
    public void setOnLoginListener(OnLoginListener onLoginListener) {
        this.onLoginListener = onLoginListener;
    }

    /**
     * Get username of socket
     *
     * @return username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Run on a different thread
     */
    @Override
    public void run() {
        /*
         * When connected send an INFO message welcoming the client
         * And log it in the console
         */
        if (!socket.isClosed()) {
            username = socket.getInetAddress().getHostAddress();
            // Set default username to IP
            System.out.println("[" + username + "] Connected to the server");
            // Connected
            connected = true;
            // Send a welcome message
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
                // Command not known
                sendMessage(Command.BAD_RESPONSE, "Unknown command");
            } else {
                // Log the action this user made
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

    /**
     * Handle all incoming massages
     *
     * @param message incoming message
     */
    private void handleMessage(Message message) {
        String payload = message.getPayload();

        switch (message.getCommand()) {
            case LOGIN:
                login(payload);
                break;
            case DOWNLOAD:
                sendFile(message);
                break;
            case QUIT:
                sendMessage(Command.GOOD_RESPONSE, "Quit successfully");
                connected = false;
                break;
            case FILE:
                if (ensureLoggedIn()) {
                    receiveFile(message);
                }
                break;
            case WHISPER:
                if (ensureLoggedIn()) {
                    whisper(message);
                }
                break;
            case HANDSHAKE:
                if (ensureLoggedIn()) {
                    handshake(payload);
                }
                break;
            case SESSION_TOKEN:
                if (ensureLoggedIn()) {
                    createSession(payload);
                }
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
                if (ensureLoggedIn() && ensureInRoom()) {
                    leaveRoom();
                }
                break;
            case BROADCAST_IN_ROOM:
                if (ensureLoggedIn() && ensureMessageGiven(message) && ensureInRoom()) {
                    talkInRoom(message);
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
     * Create a secure PM session
     *
     * @param payload [username] [token]
     */
    private void createSession(String payload) {
        String[] parts = payload.split(" ", 2);
        String username = parts[0];
        SocketProcess user = getUserFromUsername(username);
        if (user != null) {
            String token = parts[1];
            sendMessage(Command.GOOD_RESPONSE, "Token send successfully");
            user.sendMessage(Command.SESSION_TOKEN, this.username + ' ' + token);
        } else {
            sendMessage(Command.BAD_RESPONSE, "User with username '" + username + "' does not exist");
        }
    }

    /**
     * Request the public key of the user with the given username
     *
     * @param username user username
     */
    private void handshake(String username) {
        SocketProcess user = getUserFromUsername(username);
        if (user != null) {
            PublicKey publicKey = user.getPublicKey();
            sendMessage(Command.GOOD_RESPONSE, Base64.getEncoder().encodeToString(publicKey.getEncoded()));
        } else {
            sendMessage(Command.BAD_RESPONSE, "User with username '" + username + "' does not exist");
        }
    }

    /**
     * Client's Download
     * <p>
     * Server sends a file to a client
     * <p>
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
        // Filename
        String filename = parts[0];
        // Get file in /files
        File file = new File(FILE_DIR + filename);
        // If file exists
        if (file.exists()) {
            try {
                // Get file bytes
                byte[] bytes = Files.readAllBytes(file.toPath());

                // Turn bytes into Base64
                String base64 = new String(Base64.getEncoder().encode(bytes));

                // Send file
                sendMessage(Command.GOOD_RESPONSE, base64 + ' ' + Checksum.getMD5Checksum(bytes));
            } catch (Exception e) {
                System.err.println(e.getMessage());
                sendMessage(Command.BAD_RESPONSE, e.getMessage());
            }
        } else {
            sendMessage(Command.BAD_RESPONSE, String.format("Requested file '%s' not found", filename));
        }
    }

    /**
     * Client's Upload
     * <p>
     * Server received a file from a client
     * <p>
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

        // Username
        String toUsername = parts[0];

        // Get user from username
        SocketProcess to = getUserFromUsername(toUsername);
        if (to != null) {
            // Filename to download
            String filename = parts[1];
            // File bytes
            byte[] bytes = Base64.getDecoder().decode(parts[2]);
            // Checksum
            String checksum = parts[3];

            try {
                // Write file if checksums match
                writeFile(filename, bytes, checksum);
                // Send sender a good response
                sendMessage(Command.GOOD_RESPONSE, "File uploaded");

                // Ask recipient if they want to download the file
                double fileSize = bytes.length / 1024. / 1024.;
                String amountMegabytes = String.format(Locale.US, "%.4f", fileSize);
                to.sendMessage(Command.FILE, username + ' ' + filename + ' ' + amountMegabytes);
            } catch (Exception e) {
                sendMessage(Command.BAD_RESPONSE, e.getMessage());
                System.err.println(e.getMessage());
            }
        } else {
            sendMessage(Command.BAD_RESPONSE, "User with username '" + toUsername + "' does not exist");
        }
    }

    /**
     * Write a file to the storage dir (/files)
     *
     * @param filename filename
     * @param bytes    bytes
     * @param checksum checksum
     * @throws Exception exception
     */
    private void writeFile(String filename, byte[] bytes, String checksum) throws Exception {
        File downloads = new File(FILE_DIR);
        if (!downloads.exists() && !downloads.mkdir()) {
            throw new Exception(String.format("Error trying to create the '%s' directory", FILE_DIR));
        }

        // Creat file
        File file = new File(FILE_DIR + filename);
        int count = 0;
        while (file.exists()) {
            // If file exists append number to start of name
            file = new File(FILE_DIR + count++ + '_' + filename);
            // Continue doing this until filename is unique
        }

        // Compare checksum with bytes
        String fileChecksum = Checksum.getMD5Checksum(bytes);
        if (!checksum.equals(fileChecksum)) {
            throw new Exception("Uploaded file does not match given checksum");
        }

        // Write file to directory
        Files.write(file.toPath(), bytes);
    }

    /**
     * Make sure there is a payload
     *
     * @param message message
     * @return true if there is a payload
     */
    private boolean ensureMessageGiven(Message message) {
        if (message.getPayload().isEmpty()) {
            sendMessage(Command.BAD_RESPONSE, "Please give a message to send");
            return false;
        } else {
            return true;
        }
    }

    /**
     * Make sure that you are logged in
     *
     * @return true if logged in, else a message is send with a BAD_RESPONSE
     */
    private boolean ensureLoggedIn() {
        if (loggedIn) {
            return true;
        } else {
            sendMessage(Command.BAD_RESPONSE, "Please log in first");
            return false;
        }
    }

    /**
     * Make sure that you are currently in a room
     *
     * @return true if in a room, else a message is send with a BAD_RESPONSE
     */
    private boolean ensureInRoom() {
        if (room != null) {
            return true;
        } else {
            sendMessage(Command.BAD_RESPONSE, "Join a room first");
            return false;
        }
    }

    /**
     * Get a logged in user by their name
     * O(n)
     *
     * @param username username
     * @return User with username or null
     */
    private SocketProcess getUserFromUsername(String username) {
        for (SocketProcess client : clients) {
            if (client.getUsername().equals(username)) {
                return client;
            }
        }
        return null;
    }

    /**
     * Vote for a user by their username
     *
     * @param message message
     */
    private void voteKick(Message message) {
        SocketProcess user = getUserFromUsername(message.getPayload());
        if (user != null) {
            room.voteFor(this, user);
        } else {
            sendMessage(Command.BAD_RESPONSE, "No user with this username found");
        }
    }

    /**
     * Send a PM to another user
     *
     * @param message message
     */
    private void whisper(Message message) {
        String payload = message.getPayload();
        if (ensureLoggedIn()) {
            // Split message
            String[] split = payload.split(" ");
            if (payload.isEmpty() || split.length <= 1) {
                // Empty payload or only a username is a bad request
                sendMessage(Command.BAD_RESPONSE, "No username/message given");
            } else {
                String username = split[0];
                String msg = payload.substring(username.length() + 1);

                // Send msg to username
                privateMessage(username, msg);
            }
        }
    }

    /**
     * Send a PM to another user
     *
     * @param username username
     * @param message  message
     */
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

    /**
     * Login with username and public key
     *
     * @param payload payload [username] [public key]
     */
    private void login(String payload) {
        if (!loggedIn) {
            String[] parts = payload.split(" ", 2);
            // Username
            String username = parts[0];
            // Username has no spaces and just characters
            if (username.matches("\\w{3,14}")) {
                if (parts.length == 2) {
                    // Check if user with this username already exists
                    if (!usernameExists(username)) {
                        try {
                            // Turn base64 pub key into an actual Public Key
                            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(parts[1].getBytes(StandardCharsets.UTF_8)));
                            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                            publicKey = keyFactory.generatePublic(keySpec);
                            // ^ Save the public key in this client
                        } catch (Exception e) {
                            sendMessage(Command.BAD_RESPONSE, "Faulty public key");
                            return;
                        }

                        // Save the username
                        this.username = username;

                        // User is logged in
                        loggedIn = true;

                        // Reply with 200
                        sendMessage(Command.GOOD_RESPONSE, String.format("Logged in as %s", username));

                        // Tell others that you joined the server
                        Message message = new Message(Command.JOINED_SERVER, String.format("%s joined the server", username));
                        for (SocketProcess client : clients) {
                            client.sendMessage(message);
                        }

                        // Add user to client list
                        clients.add(this);

                        // Tell listener that we are logged in
                        if (onLoginListener != null) onLoginListener.loggedIn(this);
                    } else {
                        sendMessage(Command.BAD_RESPONSE, String.format("User with username '%s' is already logged in", username));
                    }
                } else {
                    sendMessage(Command.BAD_RESPONSE, "Please append a key after the username");
                }
            } else {
                sendMessage(Command.BAD_RESPONSE, "Name should be between 3 and 14 characters and should match [a-zA-Z_0-9]");
            }
        } else {
            sendMessage(Command.BAD_RESPONSE, "Please logout first");
        }
    }

    /**
     * Checks if a username exists
     *
     * @param username username
     * @return true if the username exists
     */
    private boolean usernameExists(String username) {
        for (SocketProcess client : clients) {
            if (client.username.equals(username)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A message is send
     * Alias of {@link SocketProcess#sendMessage(Message)}
     *
     * @param command command
     * @param payload payload
     */
    public void sendMessage(Command command, String payload) {
        sendMessage(new Message(command, payload));
    }

    /**
     * A message is send
     *
     * @param message message
     */
    public void sendMessage(Message message) {
        if (!socket.isClosed()) {
            // Log server sending a message to this client
            System.out.println("[SERVER -> " + username + "] " + message);
            writer.println(message.toString());
            writer.flush();
        }
    }

    /**
     * Connected is set to false this disconnects the client from the server
     */
    public void timeout() {
        sendMessage(Command.DISCONNECTED, "Connection timed out");
        connected = false;
    }

    /**
     * Sends a ping message that the client has to respond to
     */
    public void ping() {
        ponged = false;
        sendMessage(Command.PING, "");
    }

    /**
     * Checks the pong status of the user
     *
     * @return pong
     */
    public boolean hasPonged() {
        return ponged;
    }

    /**
     * The user is disconnected from the server
     * if the user is in a room they leave this room
     */
    public void disconnected() {
        if (loggedIn) {
            broadcast(new Message(Command.LEFT, "left"));
        }
        if (room != null) {
            // Leave room on disconnect
            room.leave(this);
        }
        // Remove self from client list
        clients.remove(this);
        connected = false;
    }

    /**
     * A list of all users on the server is send
     */
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

    /**
     * A list of all rooms is send
     */
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

    /**
     * A list of the users in a room is send
     */
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

    /**
     * The user joins a room
     * if the name matches the regex
     * if the user is not already in that room
     * if the room exists
     *
     * @param message message
     */
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

    /**
     * A message is send to all the users in the current room
     *
     * @param message the message that is being send
     */
    public void talkInRoom(Message message) {
        message.setPayload((username + " " + message.getPayload()).trim());

        sendMessage(Command.GOOD_RESPONSE, message.getPayload());
        room.broadcastInRoom(message);
    }

    /**
     * A room is created by the user and added to the list of rooms
     * Checks the room regex
     */
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

    /**
     * Check if a room exists
     *
     * @param roomName name of the room
     * @return true if the room exists
     */
    private boolean roomNameExists(String roomName) {
        for (Room room : rooms) {
            if (room.getRoomName().equals(roomName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The user leaves a room
     */
    public void leaveRoom() {
        room.leave(this);
        room = null;
        sendMessage(Command.GOOD_RESPONSE, "Left room");
    }

    /**
     * Clears all rooms
     */
    public void clearRoom() {
        this.room = null;
    }

    /**
     * Get the public key
     *
     * @return public key
     */
    public PublicKey getPublicKey() {
        return this.publicKey;
    }

    /**
     * Broadcasts a message to all users including yourself
     */
    public void broadcast(Message message) {
        message.setPayload((username + " " + message.getPayload()).trim());
        for (SocketProcess client : clients) {
            client.sendMessage(message);
        }
    }

    /**
     * Listens for login
     */
    public interface OnLoginListener {
        void loggedIn(SocketProcess process);
    }
}
