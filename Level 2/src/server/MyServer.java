package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class MyServer {
    public static final int TIMEOUT = -1; //5000; // -1 for no timeout, max 3.6e6 ms
    private static final int DEFAULT_PORT = 1337;
    private final ArrayList<SocketProcess> clients;
    private final ArrayList<Room> rooms;

    public MyServer() throws IOException {
        this(DEFAULT_PORT);
    }

    @SuppressWarnings("ALL")
    public MyServer(int port) throws IOException {
        clients = new ArrayList<>();
        rooms = new ArrayList<>();

        rooms.add(new Room("owo"));
        rooms.add(new Room("swag"));
        rooms.add(new Room("shrek"));

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
                public void sendRooms() {
                    StringBuilder roomList = new StringBuilder();
                    for (int i = 0; i < rooms.size(); i++) {
                        Room room = rooms.get(i);
                        roomList.append(room.toString()).append(";");
                    }
                    if (!rooms.isEmpty()) {
                        roomList.setLength(roomList.length() - 1);
                    }
                    System.out.println(roomList);

                    process.sendMessage(Command.ROOMS, roomList.toString());
                }

                @Override
                public void joinRoom(String username, Message message) {
                    String roomName = message.getPayload();
                    boolean exist = false;
                    Room current = null;
                    if (roomName.matches("\\w{3,14}")) {
                        for (Room room : rooms) {
                            if (room.contains(process)) {
                                current = room;

                                if (exist) {
                                    break;
                                }
                            }

                            if (room.getRoomName().equals(roomName)) {
                                room.addClient(process);
                                room.broadcast(new Message(Command.JOINED, username + " joined " + roomName));
                                exist = true;

                                if (current != null) {
                                    break;
                                }
                            }
                        }
                    }
                    if (!exist) {
                        process.sendMessage(Command.UNKNOWN, "Room with name '" + roomName + "' does not exist!");
                    } else if (current != null) {
                        current.removeClient(process);
                    }
                }

                @Override
                public void talkInRoom(String username, Message message) {
                    message.setPayload((username + " " + message.getPayload()).trim());
                    boolean inRoom = false;
                    for (Room room : rooms) {
                        if (room.contains(process)) {
                            room.broadcast(message);
                            inRoom = true;
                            break;
                        }
                    }

                    if (!inRoom) {
                        process.sendMessage(Command.NOT_IN_A_ROON, "You haven't joined a room to talk in");
                    }
                }

                @Override
                public void voteKick(Message message) {

                }

                @Override
                public void createRoom(Message message) {
                    String roomName = message.getPayload();
                    if (roomName.matches("\\w{3,14}")) {
                        Room room = new Room(message.getPayload());
                        rooms.add(room);
                        process.sendMessage(Command.ROOM_CREATED, room.toString());
                    } else {
                        process.sendMessage(Command.INVALID_FORMAT, "Room name should be between 3 and 14 characters, and should match [a-zA-Z_0-9]");
                    }
                }

                @Override
                public void leaveRoom(String username) {

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
