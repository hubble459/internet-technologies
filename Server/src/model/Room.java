package model;

import process.SocketProcess;

import java.util.ArrayList;

/**
 * Room
 * <p>
 * Used to manage a chat room
 */
public class Room {
    private final String roomName;
    private final ArrayList<SocketProcess> clients;
    private KickRequest kickRequest;

    public Room(String roomName) {
        this.roomName = roomName;
        this.clients = new ArrayList<>();
    }

    /**
     * Send a message to all the people in the room
     *
     * @param message to send
     */
    public void broadcastInRoom(Message message) {
        for (SocketProcess client : new ArrayList<>(clients)) {
            client.sendMessage(message);
        }
    }

    /**
     * Join room
     *
     * @param client user
     */
    public void join(SocketProcess client) {
        clients.add(client);
        broadcastInRoom(new Message(Command.JOINED_ROOM, client.getUsername() + " joined " + roomName));
        client.sendMessage(Command.GOOD_RESPONSE, client.getUsername() + " joined " + roomName);
    }

    /**
     * Leave room
     *
     * @param client user
     */
    public void leave(SocketProcess client) {
        broadcastInRoom(new Message(Command.LEFT_ROOM, client.getUsername() + " left " + roomName));
        removeClient(client);
    }

    /**
     * Kick from room
     *
     * @param client kick user
     */
    public void kick(SocketProcess client) {
        if (clients.contains(client)) {
            broadcastInRoom(new Message(Command.KICK_RESULT, "1 " + client.getUsername() + " was kicked ówò"));
            removeClient(client);
        } else {
            broadcastInRoom(new Message(Command.KICK_RESULT, "0 " + client.getUsername() + " already left the room ówò"));
        }
    }

    /**
     * Remove client from room
     *
     * @param client user
     */
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

    /**
     * Start emergency meeting
     *
     * @param starter user
     */
    public void startKick(SocketProcess starter) {
        if (kickRequest == null) {
            kickRequest = new KickRequest(this);
            starter.sendMessage(Command.GOOD_RESPONSE, roomName);
            broadcastInRoom(new Message(Command.START_KICK, "Vote kick started"));
        } else {
            starter.sendMessage(Command.BAD_RESPONSE, "A kick has already been requested");
        }
    }

    public void voteFor(SocketProcess voter, SocketProcess user) {
        if (kickRequest != null) {
            if (clients.contains(user)) {
                kickRequest.increment(voter, user);
            } else {
                voter.sendMessage(Command.BAD_RESPONSE, "No user with this username found");
            }
        } else {
            voter.sendMessage(Command.BAD_RESPONSE, "Make a kick request first");
        }
    }

    public void voteSkip(SocketProcess voter) {
        if (kickRequest != null) {
            kickRequest.skip(voter);
        } else {
            voter.sendMessage(Command.BAD_RESPONSE, "Make a kick request first");
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
