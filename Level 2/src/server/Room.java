package server;

import java.util.ArrayList;

public class Room {
    private final String roomName;
    private final ArrayList<SocketProcess> clients;
    private final ArrayList<KickRequest> kickRequests;

    public Room(String roomName) {
        this.roomName = roomName;
        this.clients = new ArrayList<>();
        this.kickRequests = new ArrayList<>();
    }

    public void broadcast(Message message) {
        for (SocketProcess client : clients) {
            client.sendMessage(message);
        }
    }

    public void join(SocketProcess client) {
        clients.add(client);
        broadcast(new Message(Command.JOINED, client.getUsername() + " joined " + roomName));
    }

    public void leave(SocketProcess client) {
        broadcast(new Message(Command.LEFT, client.getUsername() + " left " + roomName));
        clients.remove(client);
    }

    public String getRoomName() {
        return roomName;
    }

    public int size() {
        return clients.size();
    }

    public void removeKickRequest(KickRequest request) {
        kickRequests.remove(request);
    }

    public void addKickRequest(SocketProcess user) {
        if (getKickRequest(user) == null) {
            if (contains(user)) {
                broadcast(new Message(Command.VOTE_KICK, "Vote kick started for " + user.getUsername()));
                KickRequest request = new KickRequest(user, this);
                kickRequests.add(request);
            } else {
                broadcast(new Message(Command.NOT_IN_A_ROOM, "User with username '" + user.getUsername() + "' is not in this room"));
            }
        } else {
            broadcast(new Message(Command.KICK_ALREADY_REQUESTED, "User with username '" + user.getUsername() + "' is already requested to be kicked"));
        }
    }

    private KickRequest getKickRequest(SocketProcess user) {
        for (KickRequest kickRequest : kickRequests) {
            if (kickRequest.getUser().equals(user)) {
                return kickRequest;
            }
        }
        return null;
    }

    public void voteFor(SocketProcess user, boolean yes) {
        KickRequest request = getKickRequest(user);
        if (request != null) {
            if (yes) {
                request.incrementYes();
            } else {
                request.incrementNo();
            }
            broadcast(new Message(Command.VOTE_KICK, request.toString()));
        } else {
            broadcast(new Message(Command.BROADCAST, "Make a kick request first"));
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
