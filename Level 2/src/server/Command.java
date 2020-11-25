package server;

public enum Command {
    USERS("USERS"), // USRS (get the list of users)
    ROOMS("ROOMS"), // ROOMS (get the list of rooms)
    ROOM("ROOM"), // ROOM (get the list of users in room)
    JOIN_ROOM("JOIN"), // JOIN <room_id> (to join)
    LEAVE_ROOM("LEAVE"), // LEAVE (to leave room)
    BROADCAST_IN_ROOM("TALK"), // TALK <message> (to speak in room)
    CREATE_ROOM("MAKE"), // MAKE <room_name> (to create)
    WHISPER("SEND"), // SEND <username> (to private message)
    VOTE_KICK("KICK"), // KICK (to start kick)
    VOTE_SKIP("SKIP"), // SKIP (to skip vote)
    VOTE_KICK_USER("VOTE"), // KICK <username> (to vote for a user)
    LOGIN("CONN"), // CONN <username> (to login)
    QUIT("QUIT"), // QUIT (to disconnect)
    BROADCAST("BCST"), // BCST <message> (to broadcast message)
    PONG("PONG"), // PONG (send pong to notify server)
    PING("PING"), // (ping message)
    DISCONNECTED("DCST"), // DSCT?!
    INFO("INFO"), // (info message)
    UNKNOWN("400"),
    ALREADY_LOGGED_IN("401"),
    INVALID_FORMAT("402"),
    NOT_LOGGED_IN("403"),
    NOT_IN_A_ROOM("404"),
    ROOM_NAME_EXIST("405"),
    NO_KICK_REQUEST("406"),
    KICK_ALREADY_REQUESTED("407"),
    KICK_REQUEST_TIMED_OUT("408"),
    LOGGED_IN("200"),
    QUITED("201"),
    KICKED("202"),
    ROOM_CREATED("203"),
    JOINED_SERVER("204"),
    JOINED_ROOM("206"),
    LEFT("205");

    private final String command;

    Command(String command) {
        this.command = command;
    }

    @Override
    public String toString() {
        return command;
    }

    public static Command fromCommand(String command) {
        for (Command value : values()) {
            if (value.command.equals(command)) {
                return value;
            }
        }
        return null;
    }
}
