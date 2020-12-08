package server;

public enum Command {
    USERS("USERS"), // USERS (get the list of users)
    ROOMS("ROOMS"), // ROOMS (get the list of rooms)
    ROOM("ROOM"), // ROOM (get the list of users in room)
    JOIN_ROOM("JOIN"), // JOIN <room_id> (to join)
    LEAVE_ROOM("LEAVE"), // LEAVE (to leave room)
    BROADCAST_IN_ROOM("TALK"), // TALK <message> (to speak in room)
    CREATE_ROOM("MAKE"), // MAKE <room_name> (to create)
    ROOM_CREATED("MADE"), // MADE <room_name> (to broadcast)
    WHISPER("SEND"), // SEND <username> (to private message)
    VOTE_KICK("KICK"), // KICK (to start kick)
    VOTE_SKIP("SKIP"), // SKIP (to skip vote)
    VOTE_KICK_USER("VOTE"), // KICK <username> (to vote for a user)
    VOTES("VOTES"),
    LOGIN("CONN"), // CONN <username> (to login)
    QUIT("QUIT"), // QUIT (to disconnect)
    BROADCAST("BCST"), // BCST <message> (to broadcast message)
    PONG("PONG"), // PONG (send pong to notify server)
    PING("PING"), // (ping message)
    DISCONNECTED("DCST"), // Disconnected
    INFO("INFO"), // (info message)
    KICK_RESULT("KRES"), // KRES <type> <username>
    UNKNOWN("400"),
    ALREADY_LOGGED_IN("401"),
    INVALID_FORMAT("402"),
    NOT_LOGGED_IN("403"),
    NOT_IN_A_ROOM("404"),
    ROOM_NAME_EXIST("405"),
    NO_KICK_REQUEST("406"),
    KICK_ALREADY_REQUESTED("407"),
    KICK_REQUEST_TIMED_OUT("408"),
    NO_MESSAGE("409"),
    ALREADY_IN_ROOM("410"),
    MAKE_A_REQUEST_FIRST("411"),
    ALREADY_VOTED("412"),
    LOGGED_IN("200"),
    QUITED("201"),
    ROOM_CREATED_RESPONSE("203"),
    BROADCASTED("204"),
    LEAVE_ROOM_RESPONSE("205"),
    JOINED_ROOM_RESPONSE("206"),
    VOTED("207"),
    WHISPERED("208"),
    ROOM_LIST("209"),
    PEOPLE_IN_ROOM("210"),
    VOTE_KICK_STARTED("211"),
    USER_LIST("212"), // USERS RESPONSE (get the list of users)
    TALKED("213"), // USERS RESPONSE (get the list of users)
    JOINED_SERVER("JSERVER"),
    JOINED_ROOM("JROOM"),
    LEFT("LEFT");

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
