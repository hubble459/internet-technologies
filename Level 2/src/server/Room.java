package server;

import java.util.ArrayList;

public class Room {
    private final String roomName;
    private final ArrayList<SocketProcess> clients;
    private KickRequest kickRequest;

    public Room(String roomName) {
        this.roomName = roomName;
        this.clients = new ArrayList<>();
    }

    public void broadcastInRoom(Message message) {
        for (SocketProcess client : clients) {
            client.sendMessage(message);
        }
    }

    public void join(SocketProcess client) {
        clients.add(client);
        broadcastInRoom(new Message(Command.JOINED_ROOM, client.getUsername() + " joined " + roomName));
    }

    public void leave(SocketProcess client) {
        broadcastInRoom(new Message(Command.LEFT, client.getUsername() + " left " + roomName));
        removeClient(client);
    }

    public void kick(SocketProcess client) {
        if (clients.contains(client)) {
            broadcastInRoom(new Message(Command.KICKED, client.getUsername() + " was kicked ówò"));
            removeClient(client);
        } else {
            broadcastInRoom(new Message(Command.KICKED, client.getUsername() + " already left the room ówò"));
        }
    }

    private void removeClient(SocketProcess client) {
        clients.remove(client);
        client.clearRoom();
    }

    public String getRoomName() {
        return roomName;
    }

    public int size() {
        return clients.size();
    }

    public void removeKickRequest() {

        kickRequest = null;
    }

    public ArrayList<SocketProcess> getClients() {
        return clients;
    }

    public void startKick() {
        if (kickRequest == null) {
            kickRequest = new KickRequest(this);
            broadcastInRoom(new Message(Command.VOTE_KICK, "Vote kick started"));
        } else {
            broadcastInRoom(new Message(Command.KICK_ALREADY_REQUESTED, "A kick has already been requested"));
        }
    }

    public void voteFor(SocketProcess user) {
        if (kickRequest != null) {
            if (clients.contains(user)) {
                kickRequest.increment(user);
            } else {
                broadcastInRoom(new Message(Command.UNKNOWN, "No user with this username found"));
            }
        } else {
            broadcastInRoom(new Message(Command.MAKE_A_REQUEST_FIRST, "Make a kick request first"));
        }
    }

    public void voteSkip() {
        if (kickRequest != null) {
            kickRequest.skip();
        } else {
            broadcastInRoom(new Message(Command.MAKE_A_REQUEST_FIRST, "Make a kick request first"));
        }
    }

    @Override
    public String toString() {
        return roomName;
    }

    public boolean contains(SocketProcess process) {
        return clients.contains(process);
    }
}
