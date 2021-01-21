package model;

import process.SocketProcess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

/**
 * The wrapper for an emergency meeting
 */
public class KickRequest {
    // Meeting room
    private final Room room;
    // Users and amount of votes they have
    private final HashMap<SocketProcess, Integer> votes;
    // List of users who already voted
    private final ArrayList<SocketProcess> voted;
    // Timer, because the request times out after 30000 ms
    private final Timer timer;
    // Amount of skip votes
    private int skips;

    public KickRequest(Room room) {
        this.room = room;
        this.votes = new HashMap<>();
        this.voted = new ArrayList<>();
        for (SocketProcess client : room.getClients()) {
            votes.putIfAbsent(client, 0);
        }

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                kick(true);
            }
        }, 30000);
    }

    /**
     * Increment the votes of a user
     *
     * @param voter user who voted
     * @param user  user who was voted for
     */
    public void increment(SocketProcess voter, SocketProcess user) {
        if (voted.contains(voter)) {
            voter.sendMessage(Command.BAD_RESPONSE, "Already voted");
        } else {
            voter.sendMessage(Command.GOOD_RESPONSE, user.getUsername());

            voted.add(voter);

            votes.putIfAbsent(user, 0);
            votes.computeIfPresent(user, (s, integer) -> integer + 1);

            room.broadcastInRoom(new Message(Command.VOTES, this.toString()));
            kick(false);
        }
    }

    /**
     * Check if everyone voted and if so kick the user with the highest votes
     * If there a multiple users with the highest vote no-one is kicked
     *
     * @param force do kick and finish meeting, even if someone has not voted yet
     */
    public void kick(boolean force) {
        int count = votes();

        if (count >= room.size() || force) {
            SocketProcess user = getHighestVotedFor();

            if (user != null) {
                room.kick(user);
            } else {
                room.broadcastInRoom(new Message(Command.KICK_RESULT, "0 No one was kicked"));
            }
            timer.cancel();
            room.removeKickRequest();
        }
    }

    /**
     * Get the person with the most votes,
     * If there is multiple with the equal amount of votes, return null
     *
     * @return process.SocketProcess or null
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

    /**
     * Get the total amount of votes
     *
     * @return total amount of votes
     */
    private int votes() {
        int count = skips;
        for (Integer value : votes.values()) {
            count += value;
        }
        return count;
    }

    /**
     * Turn the votes into a String
     *
     * @return [username] [votes];[username] [votes];[username] [votes]
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        votes.forEach((process, integer) -> sb.append(process.getUsername()).append(" ").append(integer).append(";"));
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    /**
     * Vote skip
     *
     * @param voter who voted
     */
    public void skip(SocketProcess voter) {
        if (voted.contains(voter)) {
            voter.sendMessage(Command.BAD_RESPONSE, "Already voted");
        } else {
            voter.sendMessage(Command.GOOD_RESPONSE, "Skipped");
            voted.add(voter);
            skips++;
            room.broadcastInRoom(new Message(Command.VOTES, this.toString()));
            kick(false);
        }
    }
}
