package server;

import java.util.Timer;
import java.util.TimerTask;

public class KickRequest {
    private final SocketProcess user;
    private final Room room;
    private int yesVote = 0;
    private int noVote = 0;

    public KickRequest(SocketProcess user, Room room) {
        this.user = user;
        this.room = room;

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                room.broadcast(new Message(Command.KICK_REQUEST_TIMED_OUT, "Kick request expired for " + user.getUsername()));
                room.removeKickRequest(KickRequest.this);
            }
        }, 30000);
    }

    public SocketProcess getUser() {
        return user;
    }

    public void incrementYes() {
        ++yesVote;
        kick();
    }

    public void incrementNo() {
        ++noVote;
        kick();
    }

    public void kick() {
        if (room.size() == (yesVote + noVote)) {
            if (yesVote > noVote) {
                room.leave(user);
                user.sendMessage(Command.KICKED, user.getUsername() + " was kicked òwó");
            } else {
                user.sendMessage(Command.KICKED, user.getUsername() + " was not kicked ówò");
            }
            room.removeKickRequest(this);
        }
    }

    @Override
    public String toString() {
        return user.getUsername() + " yes: " + yesVote + " no: " + noVote + " from: " + room.size();
    }
}
