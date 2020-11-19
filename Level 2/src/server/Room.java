package server;

import java.util.ArrayList;

public class Room {
    private final String roomName;
    private final ArrayList<SocketProcess> clients;

    public Room(String roomName) {
        this.roomName = roomName;
        this.clients = new ArrayList<>();
    }

    public void broadcast(Message message) {
        for (SocketProcess client : clients) {
            client.sendMessage(message);
        }
    }

    public void addClient(SocketProcess client) {
        clients.add(client);
    }

    public void removeClient(SocketProcess client) {
        clients.remove(client);
    }

    public String getRoomName() {
        return roomName;
    }

    @Override
    public String toString() {
        return roomName;
    }

    public boolean contains(SocketProcess process) {
        return clients.contains(process);
    }
}
