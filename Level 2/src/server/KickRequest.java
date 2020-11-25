package server;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class KickRequest {
    private final Room room;
    private final HashMap<SocketProcess, Integer> votes;
    private final Timer timer;
    private int skips;

    public KickRequest(Room room) {
        this.room = room;
        this.votes = new HashMap<>();

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                kick(true);
            }
        }, 30000);
    }

    public void increment(SocketProcess user) {
        votes.putIfAbsent(user, 0);
        votes.computeIfPresent(user, (s, integer) -> integer + 1);

        room.broadcast(new Message(Command.VOTE_KICK, this.toString()));
        kick(false);
    }

    public void kick(boolean force) {
        int count = votes();

        if (count == room.size() || force) {
            SocketProcess user = getHighestVotedFor();

            if (user != null) {
                room.kick(user);
            } else {
                room.broadcast(new Message(Command.KICKED, "No one was kicked"));
            }
            timer.cancel();
            room.removeKickRequest();
        }
    }

    /**
     * Get the person with the most votes,
     * If there is multiple with the equal amount of votes, return null
     *
     * @return SocketProcess or null
     */
    private SocketProcess getHighestVotedFor() {
        int max = 0;
        SocketProcess user = null;
        for (SocketProcess process : votes.keySet()) {
            int val = votes.get(process);
            if (val > max) {
                max = val;
                user = process;
            } else if (val == max) {
                user = null;
            }
        }
        return user;
    }

    private int votes() {
        int count = skips;
        for (Integer value : votes.values()) {
            count += value;
        }
        return count;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        votes.forEach((process, integer) -> sb.append(process.getUsername()).append(" ").append(integer).append(";"));
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    public void skip() {
        skips++;
        kick(false);
    }
}
