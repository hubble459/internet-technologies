package helper.model;


public enum Command {
    // COMMANDS
    USERS("USERS"), // USERS (get the list of users)
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
    FILE("FILE"), // FILE <recipient> <filename> <base64> (to send a file)
    DOWNLOAD("DOWN"), // DOWN <filename> (To download a file)

    // NOTIFICATIONS
    INFO("INFO"), // (info message)
    PING("PING"), // (ping message)
    DISCONNECTED("DCSN"), // Disconnected (DICSONNECTEN)
    ROOM_CREATED("MADE"), // MADE <room_name> (to broadcast)
    VOTES("VOTES"), // VOTES <name> <votes>;<name> <votes>
    KICK_RESULT("KRES"), // KRES <type> <username> (to get the result of the kick)
    JOINED_SERVER("JSERVER"), // JSERVER <username> <message> (joined the server)
    JOINED_ROOM("JROOM"), // JROOM <username> <message> (joined the room)
    LEFT_ROOM("RLEFT"), // RLEFT <username> <message> (left the room)
    LEFT("LEFT"), // LEFT <username> <message> (left the server)
    //STARTED_KICK("KICK"), // (vote kick started)
    //FILE("FILE"), // FILE <from> <filename> (when you received a file)

    // RESPONSES
    GOOD_RESPONSE("200"), // Good response
    BAD_RESPONSE("400"), // Bad response

    // OTHER
    NONE(""),
    SERVER("");
    private final String command;

    Command(String command) {
        this.command = command;
    }

    public static Command fromString(String command) {
        for (Command value : values()) {
            if (value.command.equals(command)) {
                return value;
            }
        }

        if (command.startsWith("2")) {
            return GOOD_RESPONSE;
        } else if (command.startsWith("4")) {
            return BAD_RESPONSE;
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return command;
    }
}


