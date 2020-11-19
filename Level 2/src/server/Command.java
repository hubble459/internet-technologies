package server;

public enum Command {
    ROOMS("ROOMS"), // ROOMS (get the list of rooms)
    JOIN_ROOM("JOIN"), // JOIN <room_id> (to join)
    BROADCAST_IN_ROOM("TALK"), // TALK <message> (to speak in room)
    CREATE_ROOM("MAKE"), // MAKE <room_name> (to create)
    WHISPER("PM"), // PM <username> (to private message)
    VOTE_KICK("KICK"), // KICK <username> (to vote for kick)
    VOTE_KICK_TRUE("VYES"), // VYES <username> (to vote yes)
    VOTE_KICK_FALSE("VNO"), // VNO <username> (to vote no)
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
    NOT_IN_A_ROON("404"),
    LOGGED_IN("200"),
    QUITED("201"),
    ROOM_CREATED("203"),
    JOINED("204");

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
